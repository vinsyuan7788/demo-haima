package com.demo.haima.server;

import com.demo.haima.common.definition.OpCode;
import com.demo.haima.common.io.ByteBufferInputStream;
import com.demo.haima.common.runtime.Environment;
import com.demo.haima.common.serdes.jute.Record;
import com.demo.haima.common.serdes.jute.deserializer.BinaryOutputArchive;
import com.demo.haima.common.serdes.jute.request.ConnectRequest;
import com.demo.haima.common.serdes.jute.request.header.RequestHeader;
import com.demo.haima.common.serdes.jute.response.ConnectResponse;
import com.demo.haima.common.serdes.jute.serializer.BinaryInputArchive;
import com.demo.haima.common.serdes.jute.utils.RecordUtils;
import com.demo.haima.common.utility.LogUtils;
import com.demo.haima.server.connection.NioServerConnection;
import com.demo.haima.server.connection.ServerConnection;
import com.demo.haima.server.connection.factory.ServerConnectionFactory;
import com.demo.haima.server.database.YutuDatabase;
import com.demo.haima.server.database.data.structure.DataTree.ProcessTxnResult;
import com.demo.haima.server.database.transaction.CreateSessionTxn;
import com.demo.haima.server.database.transaction.header.TxnHeader;
import com.demo.haima.server.exception.MissingSessionException;
import com.demo.haima.server.jmx.bean.DataTreeMBean;
import com.demo.haima.server.jmx.bean.ServerMBean;
import com.demo.haima.server.jmx.registry.MBeanRegistry;
import com.demo.haima.server.listener.HaimaServerListener;
import com.demo.haima.server.listener.ServerListener;
import com.demo.haima.server.request.Request;
import com.demo.haima.server.request.processor.RequestFinalProcessor;
import com.demo.haima.server.request.processor.RequestPreliminaryProcessor;
import com.demo.haima.server.request.processor.RequestProcessor;
import com.demo.haima.server.request.processor.RequestSynchronousProcessor;
import com.demo.haima.server.request.processor.RequestUnimplementedProcessor;
import com.demo.haima.server.request.processor.utils.ChangeRecord;
import com.demo.haima.server.session.Session;
import com.demo.haima.server.session.expirer.SessionExpirer;
import com.demo.haima.server.session.tracker.BaviSessionTracker;
import com.demo.haima.server.session.tracker.SessionTracker;
import com.demo.haima.server.shutdown.ServerShutdownHandler;
import com.demo.haima.server.state.ServerState;
import com.demo.haima.server.statistics.provider.ServerStatisticsProvider;
import com.demo.haima.server.statistics.tracker.ServerStatisticsTracker;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Random;

/**
 * This class implements a standalone HaimaServer. It sets up the
 * following chain of RequestProcessors to process requests:
 * PrepRequestProcessor -> SyncRequestProcessor -> FinalRequestProcessor
 *
 * @author Vince Yuan
 * @date 2021/11/9
 */
public class HaimaServer extends Server implements SessionExpirer, ServerStatisticsTracker, ServerStatisticsProvider {

    private static final Logger LOG;
    /**
     * This is the secret that we use to generate passwords, for the moment it
     * is more of a sanity check.
     */
    private static final long SUPER_SECRET = 0XB3415C00L;

    static {
        // Keep these two lines together to keep the initialization order explicit
        LOG = LoggerFactory.getLogger(HaimaServer.class);
        Environment.logEnv("Server environment:", LOG);
    }

    /**
     * An interval period in milliseconds between two ticks,
     * which provides the configured value needed by
     * other component (e.g., session tracker) or configuration (e.g., minSessionTimeout, maxSessionTimeout)
     */
    private int tickTime = 3000;
    /**
     * value of -1 indicates unset, use default
     */
    private int minSessionTimeout = -1;
    /**
     * value of -1 indicates unset, use default
     */
    private int maxSessionTimeout = -1;

    private volatile ServerState serverState = ServerState.INITIAL;
    private ServerListener serverListener;
    private ServerShutdownHandler serverShutdownHandler;
    private ServerConnectionFactory serverConnectionFactory;
    private YutuDatabase database;
    private SessionTracker sessionTracker;
    private RequestProcessor requestProcessor;

    private DataTreeMBean dataTreeMBean;

    private final List<ChangeRecord> outstandingChanges = new ArrayList<>();
    /**
     * this data structure must be accessed under the outstandingChanges lock
     */
    private final Map<String, ChangeRecord> outstandingChangesForPath = new HashMap<>();

    /**
     * Creates a HaimaServer instance. Nothing is set up, use the setX
     * methods to prepare the instance (e.g., tick-time, builder, etc...)
     */
    public HaimaServer() {
        // todo v.y. stats
//        serverStats = new ServerStats(this);
        serverListener = new HaimaServerListener(this);
        LOG.info(LogUtils.getMessage("Server listener is instantiated and set"));
    }

    @Override
    public void setTickTime(int tickTime) {
        this.tickTime = tickTime;
        LOG.info(LogUtils.getMessage("Tick-time is set to {}ms"), tickTime);
    }

    @Override
    public void setMinSessionTimeout(int minSessionTimeout) {
        this.minSessionTimeout = minSessionTimeout;
        LOG.info(LogUtils.getMessage("Minimum session timeout is set to {}ms"), minSessionTimeout);
    }

    @Override
    public void setMaxSessionTimeout(int maxSessionTimeout) {
        this.maxSessionTimeout = maxSessionTimeout;
        LOG.info(LogUtils.getMessage("Maximum session timeout is set to {}ms"), maxSessionTimeout);
    }

    @Override
    public void registerServerShutdownHandler(ServerShutdownHandler serverShutdownHandler) {
        this.serverShutdownHandler = serverShutdownHandler;
        LOG.info(LogUtils.getMessage("Server shutdown handler is set"));
    }

    @Override
    public boolean canShutdown() {
        return serverState == ServerState.RUNNING || serverState == ServerState.ERROR;
    }

    @Override
    public synchronized void shutdown(boolean fullyShutdown) {
        if (!canShutdown()) {
            LOG.debug("Haima server is not running, so not proceeding to shutdown!");
            return;
        }

        setServerState(ServerState.SHUTDOWN);

        // Since sessionTracker and syncThreads poll we just have to
        // set running to false and they will detect it during the poll
        // interval.
        if (sessionTracker != null) {
            sessionTracker.shutdown();
        }

        if (requestProcessor != null) {
            requestProcessor.shutdown();
        }

        if (fullyShutdown && database != null) {
            database.clear();
        }

        // else there is no need to clear the database
        //  * When a new quorum is established we can still apply the diff
        //    on top of the same zkDb data
        //  * If we fetch a new snapshot from leader, the zkDb will be
        //    cleared anyway before loading the snapshot

        unregisterJMX();

        LOG.info(LogUtils.getMessage("Server is shut down"));
    }


    @Override
    public void setServerConnectionFactory(ServerConnectionFactory serverConnectionFactory) {
        this.serverConnectionFactory = serverConnectionFactory;
        LOG.info(LogUtils.getMessage("Server connection factory is set"));
    }

    @Override
    public ServerConnectionFactory getServerConnectionFactory() {
        return this.serverConnectionFactory;
    }

    @Override
    public void startData() {
        //check to see if database is not null
        if (database == null) {
            database = new YutuDatabase();
            LOG.info(LogUtils.getMessage("Database is instantiated and set"));
        }
        if (!database.isInitialized()) {
            loadData();
        }
    }

    @Override
    public synchronized void startup() {

        if (sessionTracker == null) {
            createSessionTracker();
        }
        startSessionTracker();

        setupRequestProcessors();

        registerJMX();

        setServerState(ServerState.RUNNING);

        notifyAll();
    }

    @Override
    public boolean isRunning() {
        return serverState == ServerState.RUNNING;
    }

    @Override
    public int getServerPort() {
        return serverConnectionFactory != null ? serverConnectionFactory.getServerPort() : -1;
    }

    /**
     * In this method, the buffer that contains the data sent from the client
     * (see {@link NioServerConnection#doIO(SelectionKey)}) is de-serialized to
     * a {@link ConnectRequest} instance, which contains necessary information
     * (e.g., session timeout) for session processing.
     *
     * @param serverConnection
     * @param incomingBuffer
     * @throws Exception
     */
    @Override
    public void processConnectRequest(ServerConnection serverConnection, ByteBuffer incomingBuffer) throws Exception {
        BinaryInputArchive bia = BinaryInputArchive.getArchive(new ByteBufferInputStream(incomingBuffer));
        ConnectRequest connReq = new ConnectRequest();
        connReq.deserialize(bia, "connect");
        // information logging here
        LOG.info(LogUtils.getMessage("[Data]", "Request received from client: {}"), RecordUtils.toString(connReq));

        if (LOG.isDebugEnabled()) {
            LOG.debug("Session establishment request from client " + serverConnection.getRemoteSocketAddress());
        }

        // todo v.y. read-only server
//        boolean readOnly = false;
//        try {
//            readOnly = bia.readBool("readOnly");
//            serverConnection.isOldClient = false;
//        } catch (IOException e) {
//            // this is ok -- just a packet from an old client which
//            // doesn't contain readOnly field
//            LOG.warn("Connection request from old client "
//                    + serverConnection.getRemoteSocketAddress()
//                    + "; will be dropped if server is in r-o mode");
//        }
//        if (readOnly == false && this instanceof ReadOnlyHaimaServer) {
//            String msg = "Refusing session request for not-read-only client "
//                    + serverConnection.getRemoteSocketAddress();
//            LOG.info(msg);
////            throw new ServerCnxn.CloseRequestException(msg);
//            throw new Exception(msg);
//        }

        byte[] passwd = connReq.getPasswd();

        int sessionTimeout = connReq.getTimeOut();
        sessionTimeout = negotiateSessionTimeout(sessionTimeout);
        serverConnection.setSessionTimeout(sessionTimeout);

        // We don't want to receive any packets until we are sure that the
        // session is setup
        serverConnection.disableReceive();

        long sessionId = connReq.getSessionId();

        if (sessionId != 0) {
            long clientSessionId = connReq.getSessionId();
            LOG.info("Client attempting to renew session 0x" + Long.toHexString(clientSessionId) + " at " + serverConnection.getRemoteSocketAddress());
            serverConnectionFactory.closeSession(sessionId);
            serverConnection.setSessionId(sessionId);
            reopenSession(serverConnection, sessionId, passwd, sessionTimeout);
        } else {
            LOG.info("Client attempting to establish new session at " + serverConnection.getRemoteSocketAddress());
            createSession(serverConnection, passwd, sessionTimeout);
        }
    }

    /**
     * In this method, the buffer that contains the data sent from the client
     * (see {@link NioServerConnection#doIO(SelectionKey)}) is de-serialized to
     * a {@link RequestHeader} instance, which will be used in the instantiation
     * of {@link Request} that will be further submitted to the request processor
     * for processing.
     *
     * @param serverConnection
     * @param incomingBuffer
     * @throws Exception
     */
    @Override
    public void processPacket(ServerConnection serverConnection, ByteBuffer incomingBuffer) throws Exception {
        // We have the request, now process and setup for next
        InputStream bbis = new ByteBufferInputStream(incomingBuffer);
        BinaryInputArchive bia = BinaryInputArchive.getArchive(bbis);
        RequestHeader requestHeader = new RequestHeader();
        requestHeader.deserialize(bia, "header");
        // information logging here
        LOG.info(LogUtils.getMessage("[Data]", "Request header received from client: {}"), RecordUtils.toString(requestHeader));

        // Through the magic of byte buffers, txn will not be
        // pointing to the start of the txn
        incomingBuffer = incomingBuffer.slice();

        Request request = new Request(serverConnection, serverConnection.getSessionId(), requestHeader.getType(), incomingBuffer);
        submitRequest(request);

        // todo v.y. stats
//        serverConnection.incrOutstandingRequests(requestHeader);
    }

    @Override
    public void expire(Session session) {
        long sessionId = session.getSessionId();
        LOG.info("Expiring session 0x" + Long.toHexString(sessionId) + ", timeout of " + session.getTimeout() + "ms exceeded");
        closeSession(sessionId);
    }

    @Override
    public long getServerId() {
        // todo v.y. unknown
        return 0;
    }

    @Override
    public void increaseNumberOfRequestInProcess() {
        numberOfRequestInProcess.incrementAndGet();
    }

    @Override
    public void decreaseNumberOfRequestsInProcess() {
        numberOfRequestInProcess.decrementAndGet();
    }

    @Override
    public int getNumberOfRequestInProcess() {
        return numberOfRequestInProcess.get();
    }

    /******************************* Setup and Shutdown *******************************/

    /**
     *  This method is used to restore sessions and data
     */
    public void loadData() {
        /*
         *
         * The following if block checks whether the database has
         * been initialized or not. Note that this method is
         * invoked by at least one other method:
         * BebincaServer#startdata.
         *
         */
        if(!database.isInitialized()){
            database.loadDataBase();
        }

        // Clean up dead sessions
        LinkedList<Long> deadSessions = new LinkedList<Long>();
        for (Long session : database.getSessions()) {
            if (database.getSessionIdAndSessionTimeoutMap().get(session) == null) {
                deadSessions.add(session);
            }
        }
        database.setDataTreeInit(true);
        for (long session : deadSessions) {
            killSession(session);
        }
    }

    /**
     * This method is used to create session tracker
     */
    private void createSessionTracker() {
        sessionTracker = new BaviSessionTracker(this, database.getSessionIdAndSessionTimeoutMap(), tickTime, 1, getServerListener());
        LOG.info(LogUtils.getMessage("Session tracker thread is instantiated and set"));
    }

    /**
     * This method is used to start session tracker
     */
    private void startSessionTracker() {
        ((BaviSessionTracker) sessionTracker).start();
        LOG.info(LogUtils.getMessage("Session tracker thread is started"));
    }

    /**
     * This method is used to set up request processors
     */
    private void setupRequestProcessors() {
        RequestProcessor finalProcessor = new RequestFinalProcessor(this);
        LOG.info(LogUtils.getMessage("Request final processor is instantiated"));
        RequestProcessor synchronousProcessor = new RequestSynchronousProcessor(this, finalProcessor);
        LOG.info(LogUtils.getMessage("Request synchronous processor thread is instantiated"));
        ((RequestSynchronousProcessor) synchronousProcessor).start();
        LOG.info(LogUtils.getMessage("Request synchronous processor thread is started"));
        requestProcessor = new RequestPreliminaryProcessor(this, synchronousProcessor);
        LOG.info(LogUtils.getMessage("Request preliminary processor thread is instantiated and set"));
        ((RequestPreliminaryProcessor) requestProcessor).start();
        LOG.info(LogUtils.getMessage("Request preliminary processor thread is started"));
    }

    private void registerJMX() {
        // register with JMX
        try {
            serverMBean = new ServerMBean(this);
            MBeanRegistry.getInstance().register(serverMBean, null);

            try {
                dataTreeMBean = new DataTreeMBean(database.getDataTree());
                MBeanRegistry.getInstance().register(dataTreeMBean, serverMBean);
            } catch (Exception e) {
                LOG.warn("Failed to register with JMX", e);
                dataTreeMBean = null;
            }
        } catch (Exception e) {
            LOG.warn("Failed to register with JMX", e);
            serverMBean = null;
        }
    }

    private void unregisterJMX() {
        // unregister from JMX
        try {
            if (dataTreeMBean != null) {
                MBeanRegistry.getInstance().unRegister(dataTreeMBean);
            }
        } catch (Exception e) {
            LOG.warn("Failed to unregister with JMX", e);
        }
        try {
            if (serverMBean != null) {
                MBeanRegistry.getInstance().unRegister(serverMBean);
            }
        } catch (Exception e) {
            LOG.warn("Failed to unregister with JMX", e);
        }
        serverMBean = null;
        dataTreeMBean = null;
    }

    /******************************* State Machine *******************************/

    /**
     * This method is used to set the server state and handler it
     * through the registered server shutdown handler
     *
     * @param serverState
     */
    public void setServerState(ServerState serverState) {
        this.serverState = serverState;
        LOG.info(LogUtils.getMessage("Server state is set to {}"), serverState);
        // Notify server state changes to the registered shutdown handler, if any.
        if (serverShutdownHandler != null) {
            serverShutdownHandler.handle(serverState);
        } else {
            LOG.error("ServerShutdownHandler is not registered, so Haima server won't take any action on ERROR or SHUTDOWN server state changes");
        }
    }

    /******************************* Session Processing *******************************/

    private void killSession(long sessionId) {
        if (sessionTracker != null) {
            sessionTracker.removeSession(sessionId);
        }
    }

    /**
     * This method is used to close session
     *
     * @param sessionId
     */
    private void closeSession(long sessionId) {
        submitRequest(null, sessionId, OpCode.closeSession, null);
    }

    /**
     * This method is used to reopen the specific session
     *
     * @param serverConnection
     * @param sessionId
     * @param sessionPassword
     * @param sessionTimeout
     */
    private void reopenSession(ServerConnection serverConnection, long sessionId, byte[] sessionPassword, int sessionTimeout) {
        if (!checkSessionPassword(sessionId, sessionPassword)) {
            finishSessionInit(serverConnection, false);
        } else {
            revalidateSession(serverConnection, sessionId, sessionTimeout);
        }
    }

    /**
     * This method is used to create a session
     *
     * @param serverConnection
     * @param sessionPassword
     * @param sessionTimeout
     */
    private long createSession(ServerConnection serverConnection, byte[] sessionPassword, int sessionTimeout) {
        long sessionId = sessionTracker.createSession(sessionTimeout);
//        Random r = new Random(sessionId ^ SUPER_SECRET);
//        r.nextBytes(sessionPassword);
        ByteBuffer timeoutBuffer = ByteBuffer.allocate(4);
        timeoutBuffer.putInt(sessionTimeout);
        serverConnection.setSessionId(sessionId);
        submitRequest(serverConnection, sessionId, OpCode.createSession, timeoutBuffer);
        return sessionId;
    }

    /**
     * This method is used to check the session password
     *
     * @param sessionId
     * @param sessionPassword
     * @return
     */
    private boolean checkSessionPassword(long sessionId, byte[] sessionPassword) {
        return sessionId != 0 && Arrays.equals(sessionPassword, generateSessionPassword(sessionId));
    }

    /**
     * This method is used to generate a session password
     *
     * @param sessionId
     * @return
     */
    private byte[] generateSessionPassword(long sessionId) {
        Random r = new Random(sessionId ^ SUPER_SECRET);
        byte[] p = new byte[16];
        r.nextBytes(p);
        return p;
    }

    /**
     * This method is used to finish session initialization. <br/>
     * In this method, a {@link ConnectResponse} is instantiated with necessary information
     * regarding session (e.g., session ID, session password, etc.). Then this {@link ConnectResponse}
     * instance will be serialized and eventually written to a {@link ByteArrayOutputStream} instance,
     * whose byte array format will be further wrap into a byte buffer that needs to sent to the client.
     * Then this byte buffer will be added from the end of {@link NioServerConnection#outgoingBufferQueue}.
     * To add the byte buffer from the end of {@link NioServerConnection#outgoingBufferQueue},
     * see {@link NioServerConnection#internalSendBuffer(ByteBuffer)}.
     * [Process]
     *
     * @param serverConnection
     * @param validSession
     */
    public void finishSessionInit(ServerConnection serverConnection, boolean validSession) {
        // register with JMX
        try {
            if (validSession) {
                serverConnectionFactory.registerConnection(serverConnection);
            }
        } catch (Exception e) {
            LOG.warn("Failed to register with JMX", e);
        }

        try {
            ConnectResponse rsp = new ConnectResponse(0,
                    validSession ? serverConnection.getSessionTimeout() : 0,
                    // send 0 if session is no longer valid
                    validSession ? serverConnection.getSessionId() : 0,
                    validSession ? generateSessionPassword(serverConnection.getSessionId()) : new byte[16]
            );
            // information logging here
            LOG.info(LogUtils.getMessage("[Data]", "Response sent to client: {}"), RecordUtils.toString(rsp));

            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            BinaryOutputArchive bos = BinaryOutputArchive.getArchive(baos);
            bos.writeInt(-1, "len");
            rsp.serialize(bos, "connect");
            // todo v.y. read-only server
//            if (!serverConnection.isOldClient) {
//                bos.writeBool(this instanceof ReadOnlyHaimaServer, "readOnly");
//            }
            baos.close();
            ByteBuffer bb = ByteBuffer.wrap(baos.toByteArray());
            bb.putInt(bb.remaining() - 4).rewind();
            serverConnection.sendBuffer(bb);

            if (!validSession) {
                LOG.info("Invalid session 0x"
                        + Long.toHexString(serverConnection.getSessionId())
                        + " for client "
                        + serverConnection.getRemoteSocketAddress()
                        + ", probably expired");
                serverConnection.sendBuffer(ServerConnectionFactory.CLOSE_CONNECTION_BUFFER);
            } else {
                LOG.info("Established session 0x"
                        + Long.toHexString(serverConnection.getSessionId())
                        + " with negotiated timeout " + serverConnection.getSessionTimeout()
                        + " for client "
                        + serverConnection.getRemoteSocketAddress());
                serverConnection.enableRecv();
            }
        } catch (Exception e) {
            LOG.warn("Exception while establishing session, closing", e);
            serverConnection.close();
        }
    }

    /**
     * This method is used to revalidate the specific session
     *
     * @param serverConnection
     * @param sessionId
     * @param sessionTimeout
     */
    private void revalidateSession(ServerConnection serverConnection, long sessionId, int sessionTimeout) {
        boolean touched = sessionTracker.touchSession(sessionId, sessionTimeout);
        // todo v.y. trace
//        if (LOG.isTraceEnabled()) {
//            BETrace.logTraceMessage(LOG, BETrace.SESSION_TRACE_MASK,
//                    "Session 0x" + Long.toHexString(sessionId) +
//                            " is valid: " + rc);
//        }
        finishSessionInit(serverConnection, touched);
    }

    /**
     * This method is used to touch the session from the connection
     *
     * @param serverConnection
     * @throws MissingSessionException
     */
    private void touchSession(ServerConnection serverConnection) throws MissingSessionException {
        if (serverConnection == null) {
            return;
        }
        long sessionId = serverConnection.getSessionId();
        int sessionTimeout = serverConnection.getSessionTimeout();
        if (!sessionTracker.touchSession(sessionId, sessionTimeout)) {
            throw new MissingSessionException(
                    "No session with sessionid 0x" + Long.toHexString(sessionId)
                            + " exists, probably expired and removed");
        }
    }

    /******************************* Request Processing *******************************/

    /**
     * This method is used to re-decide the session timeout from request
     *
     * @param sessionTimeout
     * @return
     */
    private int negotiateSessionTimeout(int sessionTimeout) {
        int minSessionTimeout = getMinSessionTimeout();
        if (sessionTimeout < minSessionTimeout) {
            sessionTimeout = minSessionTimeout;
        }
        int maxSessionTimeout = getMaxSessionTimeout();
        if (sessionTimeout > maxSessionTimeout) {
            sessionTimeout = maxSessionTimeout;
        }
        return sessionTimeout;
    }

    /**
     * This method is used to submit a request
     *
     * @param serverConnection
     * @param sessionId
     * @param type
     * @param incomingBuffer
     */
    private void submitRequest(ServerConnection serverConnection, long sessionId, int type, ByteBuffer incomingBuffer) {
        Request request = new Request(serverConnection, sessionId, type, incomingBuffer);
        submitRequest(request);
    }

    /**
     * This method is used to submit a request
     *
     * @param request
     */
    private void submitRequest(Request request) {
        if (requestProcessor == null) {
            synchronized (this) {
                try {
                    // Since all requests are passed to the request
                    // processor it should wait for setting up the request
                    // processor chain. The state will be updated to RUNNING
                    // after the setup.
                    while (serverState == ServerState.INITIAL) {
                        wait(1000);
                    }
                } catch (InterruptedException e) {
                    LOG.warn("Unexpected interruption", e);
                }
                if (requestProcessor == null || serverState != ServerState.RUNNING) {
                    throw new RuntimeException("Not started");
                }
            }
        }
        try {
            touchSession(request.getServerConnection());
            boolean validPacket = Request.isValid(request.getType());
            if (validPacket) {
                requestProcessor.processRequest(request);
                if (request.getServerConnection() != null) {
                    increaseNumberOfRequestInProcess();
                }
            } else {
                LOG.warn("Received packet at server of unknown type " + request.getType());
                RequestUnimplementedProcessor requestUnimplementedProcessor = new RequestUnimplementedProcessor();
                requestUnimplementedProcessor.processRequest(request);
            }
        } catch (Exception e) {
            // todo v.y. ex
        }
    }

    /******************************* Getter and Setter *******************************/

    public int getTickTime() {
        return tickTime;
    }

    public int getMinSessionTimeout() {
        return minSessionTimeout == -1 ? tickTime * 2 : minSessionTimeout;
    }

    public int getMaxSessionTimeout() {
        return maxSessionTimeout == -1 ? tickTime * 20 : maxSessionTimeout;
    }

    public ServerListener getServerListener() {
        return this.serverListener;
    }

    public YutuDatabase getDatabase() {
        return database;
    }

    public SessionTracker getSessionTracker() {
        return sessionTracker;
    }

    public List<ChangeRecord> getOutstandingChanges() {
        return outstandingChanges;
    }

    public Map<String, ChangeRecord> getOutstandingChangesForPath() {
        return outstandingChangesForPath;
    }

    /******************************* Transaction Processing *******************************/

    /**
     * This method is used to process a transaction
     *
     * @param transactionHeader
     * @param transaction
     * @return
     */
    public ProcessTxnResult processTransaction(TxnHeader transactionHeader, Record transaction) {
        int opCode = transactionHeader.getType();
        long sessionId = transactionHeader.getClientId();
        ProcessTxnResult rc = getDatabase().processTransaction(transactionHeader, transaction);
        if (opCode == OpCode.createSession) {
            if (transaction instanceof CreateSessionTxn) {
                CreateSessionTxn cst = (CreateSessionTxn) transaction;
                sessionTracker.addSession(sessionId, cst.getTimeOut());
            } else {
                LOG.warn("*****>>>>> Got " + transaction.getClass() + " " + transaction.toString());
            }
        } else if (opCode == OpCode.closeSession) {
            sessionTracker.removeSession(sessionId);
        }
        return rc;
    }

    /******************************* Utility Method *******************************/

    /**
     * This method is used to get the port of client to request processor
     *
     * @return
     */
    public int getClientPortOfRequestProcessor() {
        return serverConnectionFactory != null ? serverConnectionFactory.getServerPort() : -1;
    }
}
