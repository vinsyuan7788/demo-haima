package com.demo.haima.client.connection;

import com.demo.haima.client.Client;
import com.demo.haima.client.callback.asynchronous.AsyncCallback;
import com.demo.haima.client.callback.asynchronous.Children2Callback;
import com.demo.haima.client.callback.asynchronous.ChildrenCallback;
import com.demo.haima.client.callback.asynchronous.DataCallback;
import com.demo.haima.client.callback.asynchronous.MultiCallback;
import com.demo.haima.client.callback.asynchronous.StatCallback;
import com.demo.haima.client.callback.asynchronous.StringCallback;
import com.demo.haima.client.callback.asynchronous.VoidCallback;
import com.demo.haima.client.connection.packet.Packet;
import com.demo.haima.client.connection.socket.ClientConnectionSocket;
import com.demo.haima.client.connection.socket.NioClientConnectionSocket;
import com.demo.haima.client.connection.state.ClientConnectionState;
import com.demo.haima.client.exception.EndOfStreamException;
import com.demo.haima.client.exception.ServerFoundException;
import com.demo.haima.client.exception.SessionExpiredException;
import com.demo.haima.client.exception.SessionTimeoutException;
import com.demo.haima.client.host.HostProvider;
import com.demo.haima.common.definition.OpCode;
import com.demo.haima.common.definition.OpResult;
import com.demo.haima.common.definition.ReplyCode;
import com.demo.haima.common.exception.HaimaException;
import com.demo.haima.common.exception.logger.UncaughtExceptionLogger;
import com.demo.haima.common.io.ByteBufferInputStream;
import com.demo.haima.common.serdes.jute.Record;
import com.demo.haima.common.serdes.jute.request.ConnectRequest;
import com.demo.haima.common.serdes.jute.request.header.RequestHeader;
import com.demo.haima.common.serdes.jute.response.CreateResponse;
import com.demo.haima.common.serdes.jute.response.ExistsResponse;
import com.demo.haima.common.serdes.jute.response.GetChildren2Response;
import com.demo.haima.common.serdes.jute.response.GetChildrenResponse;
import com.demo.haima.common.serdes.jute.response.GetDataResponse;
import com.demo.haima.common.serdes.jute.response.MultiResponse;
import com.demo.haima.common.serdes.jute.response.SetDataResponse;
import com.demo.haima.common.serdes.jute.response.header.ResponseHeader;
import com.demo.haima.common.serdes.jute.serializer.BinaryInputArchive;
import com.demo.haima.common.serdes.jute.utils.RecordUtils;
import com.demo.haima.common.utility.LogUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.ConnectException;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.nio.ByteBuffer;
import java.util.LinkedList;
import java.util.List;
import java.util.Random;
import java.util.concurrent.LinkedBlockingQueue;

/**
 * This class manages the socket i/o for the client. ClientCnxn maintains a list
 * of available servers to connect to and "transparently" switches servers it is
 * connected to as needed.
 *
 * @author Vince Yuan
 * @date 2021/11/12
 */
public class ClientConnection {

    private static final Logger LOG = LoggerFactory.getLogger(ClientConnection.class);

    protected final int clientPort = 8484;

    private final Client client;
    private final HostProvider hostProvider;
    private final SendThread sendThread;
    private final EventThread eventThread;

    private long sessionId;
    private byte[] sessionPasswd;
    private final int sessionTimeout;
    private String chrootPath;
    private int connectTimeout;
    private int readTimeout;
    /**
     * If true, the connection is allowed to go to r-o mode. This field's value
     * is sent, besides other data, during session creation handshake. If the
     * server on the other side of the wire is partitioned it'll accept
     * read-only clients only.
     */
    private boolean readOnly;

    private volatile ClientConnectionState clientConnectionState = ClientConnectionState.NOT_CONNECTED;

    /**
     * These are the packets that need to be sent.
     */
    public final LinkedList<Packet> outgoingPacketQueue = new LinkedList<>();
    /**
     * These are the packets that have been sent and are waiting for a response.
     */
    public final LinkedList<Packet> pendingPacketQueue = new LinkedList<Packet>();

    /**
     * Set to true when close is called. Latches the connection such that we
     * don't attempt to re-connect to the server if in the middle of closing the
     * connection (client sends session disconnect to server as part of close
     * operation)
     */
    private volatile boolean closing = false;

    /**
     * The timeout in ms the client negotiated with the server. This is the
     * "real" timeout, not the timeout request by the client (which may have
     * been increased/decreased by the server which applies bounds to this
     * value.
     */
    private volatile int negotiatedSessionTimeout;

    /**
     * Is set to true when a connection to a r/w server is established for the
     * first time; never changed afterwards.
     * <p>
     * Is used to handle situations when client without sessionId connects to a
     * read-only server. Such client receives "fake" sessionId from read-only
     * server, but this sessionId is invalid for other servers. So when such
     * client finds a r/w server, it sends 0 instead of fake sessionId during
     * connection handshake and establishes new, valid session.
     * <p>
     * If this field is false (which implies we haven't seen r/w server before)
     * then non-zero sessionId is fake, otherwise it is valid.
     */
    private volatile boolean seenServerBefore = false;

    /**
     * Creates a connection object. The actual network connect doesn't get
     * established until needed. The start() instance method must be called
     * subsequent to construction.
     *
     * @param chrootPath - the chroot of this client.
     * @param hostProvider
     *                the list of Haima servers to connect to
     * @param sessionTimeout
     *                the timeout for connections.
     * @param client
     *                the Haima client object that this connection is related to.
     * @param clientConnectionSocket
     *                the socket implementation used (e.g. NIO/Netty)
     * @param canBeReadOnly
     *                whether the connection is allowed to go to read-only
     *                mode in case of partitioning
     */
    public ClientConnection(String chrootPath, HostProvider hostProvider, int sessionTimeout, Client client, ClientConnectionSocket clientConnectionSocket, boolean canBeReadOnly) {
        this(chrootPath, hostProvider, sessionTimeout, client, clientConnectionSocket, 0, new byte[16], canBeReadOnly);
    }

    /**
     * Creates a connection object. The actual network connect doesn't get
     * established until needed. The start() instance method must be called
     * subsequent to construction.
     *
     * @param chrootPath - the chroot of this client.
     * @param hostProvider
     *                the list of Haima servers to connect to
     * @param sessionTimeout
     *                the timeout for connections.
     * @param client
     *                the Haima client object that this connection is related to.
     * @param clientConnectionSocket
     *                the socket implementation used (e.g. NIO/Netty)
     * @param sessionId session id if re-establishing session
     * @param sessionPasswd session passwd if re-establishing session
     * @param canBeReadOnly
     *                whether the connection is allowed to go to read-only
     *                mode in case of partitioning
     */
    public ClientConnection(String chrootPath, HostProvider hostProvider, int sessionTimeout, Client client, ClientConnectionSocket clientConnectionSocket, long sessionId, byte[] sessionPasswd, boolean canBeReadOnly) {
        this.client = client;
        LOG.info(LogUtils.getMessage("Client is set"));
        this.sessionId = sessionId;
        this.sessionPasswd = sessionPasswd;
        this.sessionTimeout = sessionTimeout;
        this.hostProvider = hostProvider;
        LOG.info(LogUtils.getMessage("Host provider is set"));
        this.chrootPath = chrootPath;

        connectTimeout = sessionTimeout / hostProvider.getNumberOfServerAddress();
        readTimeout = sessionTimeout * 2 / 3;
        readOnly = canBeReadOnly;

        sendThread = new SendThread(clientConnectionSocket);
        LOG.info(LogUtils.getMessage("Send thread is instantiated and set"));
        eventThread = new EventThread();
        LOG.info(LogUtils.getMessage("Event thread is instantiated and set"));
    }

    /******************************* Setup and Shutdown *******************************/

    /**
     * This method is used to start the thread maintained by this connection
     */
    public void start() {
        sendThread.start();
        LOG.info(LogUtils.getMessage("Send thread is started"));
        eventThread.start();
        LOG.info(LogUtils.getMessage("Event thread is started"));
    }

    /**
     * This method is used to close the connection, which includes
     * send session disconnect to the server, shutdown the send/event threads.
     *
     * @throws IOException
     */
    public void close() throws IOException {
        if (LOG.isDebugEnabled()) {
            LOG.debug("Closing client for session: 0x"
                    + Long.toHexString(getSessionId()));
        }

        try {
            RequestHeader h = new RequestHeader();
            h.setType(OpCode.closeSession);
            submitRequest(h, null, null);
        } catch (Exception e) {
            // ignore, close the send/event threads
        } finally {
            disconnect();
        }
    }

    /**
     * This method is used to shutdown the send/event threads.
     * This method should not be called directly - rather it should be called as part of close operation.
     * This method is primarily here to allow the tests to verify disconnection behavior.
     */
    private void disconnect() {
        if (LOG.isDebugEnabled()) {
            LOG.debug("Disconnecting client for session: 0x"
                    + Long.toHexString(getSessionId()));
        }
        sendThread.close();
        eventThread.queueEventOfDeath();
    }

    /******************************* Request Processing *******************************/

    /**
     * This method is used to submit a request. <br/>
     * In this method, when a request is submitted, the corresponding packet
     * that maintains this request will cause current thread to wait.
     * This will ensure current thread will wait until the packet notifies it to wake up.
     * To see the packet notifying threads to wake up, see {@link #finishQueueingPacket(Packet)}.
     * [Process]
     *
     * @param requestHeader
     * @param request
     * @param response
     * @return
     * @throws Exception
     */
    public ResponseHeader submitRequest(RequestHeader requestHeader, Record request, Record response) throws Exception {
        ResponseHeader responseHeader = new ResponseHeader();
        Packet packet = queuePacket(requestHeader, responseHeader, request, response, null, null, null, null);
        synchronized (packet) {
            while (!packet.getFinished()) {
                packet.wait();
            }
        }
        return responseHeader;
    }

    /**
     * This method is used to queue a packet. <br/>
     * In this method, a packet will be instantiated by maintaining
     * a {@link RequestHeader}, {@link Record}-typed request,
     * {@link ResponseHeader} and {@link Record}-typed response
     * that will be used to create a buffer to send to the server.
     * This packet that needs to be sent to the server will be
     * added from the end of {@link #outgoingPacketQueue}.
     * To remove this packet from {@link #outgoingPacketQueue},
     * see {@link NioClientConnectionSocket#findSendablePacket(LinkedList)}.
     * [Process]
     *
     * @param requestHeader
     * @param responseHeader
     * @param request
     * @param response
     * @param callback
     * @param clientPath
     * @param serverPath
     * @param context
     * @return
     */
    private Packet queuePacket(RequestHeader requestHeader, ResponseHeader responseHeader, Record request, Record response, AsyncCallback callback, String clientPath, String serverPath, Object context) {

        Packet packet;

        // Note that we do not generate the Xid for the packet yet. It is
        // generated later at send-time, by an implementation of ClientCnxnSocket::doIO(),
        // where the packet is actually sent.
        synchronized (outgoingPacketQueue) {
            packet = new Packet(requestHeader, responseHeader, request, response);
            packet.setClientPath(clientPath);
            packet.setServerPath(serverPath);
            packet.setCallback(callback);
            packet.setContext(context);
            if (!clientConnectionState.isAlive() || closing) {
                processPacketForConnectionLoss(packet);
            } else {
                // If the client is asking to close the session, then mark as closing
                if (requestHeader.getType() == OpCode.closeSession) {
                    closing = true;
                }
                packet.createByteBuffer();
                outgoingPacketQueue.add(packet);
            }
        }
        sendThread.getClientConnectionSocket().wakeupConnection();
        return packet;
    }

    /**
     * This method is used to process the packet for connection loss
     * 
     * @param packet
     */
    private void processPacketForConnectionLoss(Packet packet) {
        if (packet.getResponseHeader() == null) {
            return;
        }
        switch (clientConnectionState) {
            case CLOSED:
                packet.getResponseHeader().setErr(HaimaException.Code.SESSIONEXPIRED.intValue());
                break;
            default:
                packet.getResponseHeader().setErr(HaimaException.Code.CONNECTIONLOSS.intValue());
        }
        finishQueueingPacket(packet);
    }

    /**
     * This method is used to finish queueing packet. <br/>
     * In this method, the packet will notify all waiting threads to wake up
     * (the actual number of thread is usually one, here notifies all just in case).
     * To see the threads waiting for the packet, see {@link #submitRequest(RequestHeader, Record, Record)}.
     * [Process]
     *
     * @param packet
     */
    private void finishQueueingPacket(Packet packet) {
        if (packet.getContext() == null) {
            synchronized (packet) {
                packet.setFinished(true);
                packet.notifyAll();
            }
        } else {
            packet.setFinished(true);
            eventThread.queuePacket(packet);
        }
    }

    /******************************* Getter and Setter *******************************/

    /**
     * The session id for this Haima client connection. The value returned is
     * not valid until the client connects to a server and may change after a
     * re-connect.
     *
     * This method is NOT thread safe
     *
     * @return current session id
     */
    public long getSessionId() {
        return sessionId;
    }

    /**
     * The negotiated session timeout for this Haima client connection. The
     * value returned is not valid until the client connects to a server and
     * may change after a re-connect.
     *
     * This method is NOT thread safe
     *
     * @return current negotiated session timeout
     */
    public int getNegotiatedSessionTimeout() {
        return negotiatedSessionTimeout;
    }

    /******************************* State Machine *******************************/

    public void setClientConnectionState(ClientConnectionState clientConnectionState) {
        this.clientConnectionState = clientConnectionState;
        LOG.info(LogUtils.getMessage("Client connection state is set to {}"), clientConnectionState);
    }

    public ClientConnectionState getClientConnectionState() {
        return clientConnectionState;
    }

    /******************************* Utility Method *******************************/

    /**
     * This method is used to guard against creating "-EventThread-EventThread-EventThread-..." thread
     * names when Haima object is being created.
     */
    private static String makeThreadName(String suffix) {
        String name = Thread.currentThread().getName().replaceAll("-EventThread", "");
        return name + suffix;
    }

    /******************************* Thread Definition *******************************/

    /**
     * This class services the outgoing request queue and generates the heart
     * beats. It also spawns the ReadThread.
     */
    public class SendThread extends UncaughtExceptionLogger {

        private final ClientConnectionSocket clientConnectionSocket;

        /**
         * The server address to ping by this client
         */
        private InetSocketAddress serverAddressToPing;
        /**
         * A flat that signifies if this client is the first-time connecting to the server
         */
        private boolean isFirstConnect = true;

        private long lastPingSentNs;

        private Random r = new Random(System.nanoTime());

        private final static int minPingServerTimeout = 100;
        private final static int maxPingServerTimeout = 60000;
        private int pingServerTimeout = minPingServerTimeout;

        private static final String RETRY_CONN_MSG = ", closing socket connection and attempting reconnect";

        public SendThread(ClientConnectionSocket clientConnectionSocket) {
            super(makeThreadName("-SendThread()"));
            setClientConnectionState(ClientConnectionState.CONNECTING);
            this.clientConnectionSocket = clientConnectionSocket;
            LOG.info(LogUtils.getMessage("Client connection socket is set"));
            setDaemon(true);
        }

        @Override
        public void run() {
            LOG.info(LogUtils.getMessage("Send thread starts running"));
            clientConnectionSocket.setSendThreadAndSessionId(this, sessionId);
            clientConnectionSocket.updateNow();
            clientConnectionSocket.updateLastSendAndHeard();
            long lastPingServer = System.currentTimeMillis();
            final int MAX_SEND_PING_INTERVAL = 10000; //10 seconds
            while (clientConnectionState.isAlive()) {
                try {
                    if (!clientConnectionSocket.isConnected()) {
                        if (!isFirstConnect) {
                            try {
                                Thread.sleep(r.nextInt(1000));
                            } catch (InterruptedException e) {
                                LOG.warn("Unexpected exception", e);
                            }
                        }
                        // don't re-establish connection if we are closing
                        if (closing || !clientConnectionState.isAlive()) {
                            break;
                        }
                        startConnect();
                        clientConnectionSocket.updateLastSendAndHeard();
                    }

                    int to;
                    if (clientConnectionState.isConnected()) {
                        to = readTimeout - clientConnectionSocket.getIdleRecv();
                    } else {
                        to = connectTimeout - clientConnectionSocket.getIdleRecv();
                    }

                    if (to <= 0) {
                        String warnInfo = "Client session timed out, have not heard from server in " + clientConnectionSocket.getIdleRecv() + "ms" + " for sessionid 0x" + Long.toHexString(sessionId);
                        LOG.warn(warnInfo);
                        throw new SessionTimeoutException(warnInfo);
                    }
                    if (clientConnectionState.isConnected()) {
                        // 1000(1 second) is to prevent race condition missing to send the second ping
                        // also, make sure not to send too many pings when readTimeout is small
                        int timeToNextPing = readTimeout / 2 - clientConnectionSocket.getIdleSend() - ((clientConnectionSocket.getIdleSend() > 1000) ? 1000 : 0);
                        //send a ping request either time is due or no packet sent out within MAX_SEND_PING_INTERVAL
                        if (timeToNextPing <= 0 || clientConnectionSocket.getIdleSend() > MAX_SEND_PING_INTERVAL) {
                            sendPing();
                            clientConnectionSocket.updateLastSend();
                        } else {
                            if (timeToNextPing < to) {
                                to = timeToNextPing;
                            }
                        }
                    }

                    // If we are in read-only mode, seek for read/write server
                    if (clientConnectionState == ClientConnectionState.CONNECTED_READONLY) {
                        long now = System.currentTimeMillis();
                        int idlePingServer = (int) (now - lastPingServer);
                        if (idlePingServer >= pingServerTimeout) {
                            lastPingServer = now;
                            idlePingServer = 0;
                            pingServerTimeout = Math.min(2 * pingServerTimeout, maxPingServerTimeout);
                            pingServerForReadOnly();
                        }
                        to = Math.min(to, pingServerTimeout - idlePingServer);
                    }

                    clientConnectionSocket.doTransport(to, pendingPacketQueue, outgoingPacketQueue, ClientConnection.this);
                } catch (Throwable e) {
                    if (closing) {
                        if (LOG.isDebugEnabled()) {
                            // closing so this is expected
                            LOG.debug("An exception was thrown while closing send thread for session 0x"
                                    + Long.toHexString(getSessionId())
                                    + " : " + e.getMessage());
                        }
                        break;
                    } else {
                        // this is ugly, you have a better way speak up
                        if (e instanceof SessionExpiredException) {
                            LOG.info(e.getMessage() + ", closing socket connection");
                        } else if (e instanceof SessionTimeoutException) {
                            LOG.info(e.getMessage() + RETRY_CONN_MSG);
                        } else if (e instanceof EndOfStreamException) {
                            LOG.info(e.getMessage() + RETRY_CONN_MSG);
                        } else if (e instanceof ServerFoundException) {
                            LOG.info(e.getMessage());
                        } else {
                            LOG.warn("Session 0x" + Long.toHexString(getSessionId()) + " for server " + clientConnectionSocket.getRemoteSocketAddress() + ", unexpected error" + RETRY_CONN_MSG, e);
                        }
                        cleanup();

                        clientConnectionSocket.updateNow();
                        clientConnectionSocket.updateLastSendAndHeard();
                    }
                }
            }
            cleanup();
            clientConnectionSocket.close();
            // todo v.y. trace
//            BETrace.logTraceMessage(LOG, BETrace.getTextTraceLevel(), "SendThread exited loop for session: 0x" + Long.toHexString(getSessionId()));
            LOG.info(LogUtils.getMessage("Send thread completes running"));
        }

        /******************************* Setup and Shutdown *******************************/

        /**
         * This method is used to prime the connection by submitting a ConnectRequest
         */
        public void primeConnection() {
            LOG.info("Socket connection established to " + clientConnectionSocket.getRemoteSocketAddress() + ", initiating session");
            isFirstConnect = false;
            long sessionId = 0;
            ConnectRequest conReq = new ConnectRequest(0, sessionTimeout, sessionId, sessionPasswd);
            synchronized (outgoingPacketQueue) {
                // We add backwards since we are pushing into the front
                // Only send if there's a pending watch
                // TODO: here we have the only remaining use of haima in this class. It's to be eliminated!
                outgoingPacketQueue.addFirst(new Packet(null, null, conReq, null, readOnly));
            }
            clientConnectionSocket.enableReadWrite();
            if (LOG.isDebugEnabled()) {
                LOG.debug("Session establishment request sent on " + clientConnectionSocket.getRemoteSocketAddress());
            }
        }

        /******************************* Setup and Shutdown *******************************/

        /**
         * This method is used to start connecting to the server
         *
         * @throws IOException
         */
        private void startConnect() throws Exception {
            setClientConnectionState(ClientConnectionState.CONNECTING);

            InetSocketAddress serverAddressToConnect;
            if (serverAddressToPing != null) {
                serverAddressToConnect = serverAddressToPing;
                serverAddressToPing = null;
            } else {
                serverAddressToConnect = hostProvider.getNextServerAddress(1000);
            }

            setName(getName().replaceAll("\\(.*\\)", "(" + serverAddressToConnect.getHostName() + ":" + serverAddressToConnect.getPort() + ")"));

            // todo v.y. log
//            logStartConnect(addr);

            clientConnectionSocket.connect(serverAddressToConnect);
        }

        /**
         * This method is used to clean up the resources (channel, socket, selector, all kinds of queue, etc.)
         */
        private void cleanup() {
            clientConnectionSocket.cleanup();
            synchronized (pendingPacketQueue) {
                for (Packet p : pendingPacketQueue) {
                    processPacketForConnectionLoss(p);
                }
                pendingPacketQueue.clear();
            }
            synchronized (outgoingPacketQueue) {
                for (Packet p : outgoingPacketQueue) {
                    processPacketForConnectionLoss(p);
                }
                outgoingPacketQueue.clear();
            }
        }

        /**
         * This method is used to close the connection
         */
        private void close() {
            setClientConnectionState(ClientConnectionState.CLOSED);
            clientConnectionSocket.wakeupConnection();
        }

        /******************************* Data Processing *******************************/

        /**
         * This method is used to read the response data stored in the buffer. <br/>
         * In this method, we have the buffer that contains the data sent from the server
         * (see {@link NioClientConnectionSocket#doIO(List, LinkedList, ClientConnection)}),
         * where the header part will be de-serialized to {@link ResponseHeader} instance,
         * then the response part will be de-serialized to the {@link Record}-typed response
         * field of the packet that has been responded by the server. This packet will be removed
         * from {@link #pendingPacketQueue}.
         * To add this packet from the end of {@link #pendingPacketQueue},
         * see {@link NioClientConnectionSocket#doIO(List, LinkedList, ClientConnection)}.
         * [Process]
         *
         * @param incomingBuffer
         * @throws IOException
         */
        public void readResponse(ByteBuffer incomingBuffer) throws IOException {

            ByteBufferInputStream bbis = new ByteBufferInputStream(incomingBuffer);
            BinaryInputArchive bbia = BinaryInputArchive.getArchive(bbis);
            ResponseHeader responseHeader = new ResponseHeader();
            responseHeader.deserialize(bbia, "header");
            // information logging here
            LOG.info(LogUtils.getMessage("[Data]", "Response header received from server: {}"), RecordUtils.toString(responseHeader));

            if (responseHeader.getBeid() == ReplyCode.pingReply) {
                // -2 is the type for pings
                if (LOG.isDebugEnabled()) {
                    LOG.debug("Got ping response for sessionid: 0x" + Long.toHexString(sessionId) + " after " + ((System.nanoTime() - lastPingSentNs) / 1000000) + "ms");
                }
                return;
            }
            if (responseHeader.getBeid() == ReplyCode.exceptionReply) {
                // -4 is the type for exception
                LOG.warn("Got exception in sessionid:0x" + Long.toHexString(sessionId));
                return;
            }
            if (responseHeader.getBeid() == ReplyCode.notificationReply) {
                // -1 means notification
                if (LOG.isDebugEnabled()) {
                    LOG.debug("Got notification sessionid:0x" + Long.toHexString(sessionId));
                }
                //TODO notification logic
                return;
            }

            Packet packetRespondedByServer;
            synchronized (pendingPacketQueue) {
                if (pendingPacketQueue.size() == 0) {
                    throw new IOException("Nothing in the queue, but got " + responseHeader.getBeid());
                }
                packetRespondedByServer = pendingPacketQueue.remove();
            }

            try {
                packetRespondedByServer.getResponseHeader().setErr(responseHeader.getErr());
                if (packetRespondedByServer.getResponse() != null && responseHeader.getErr() == 0) {
                    packetRespondedByServer.getResponse().deserialize(bbia, "response");
                    // information logging here
                    LOG.info(LogUtils.getMessage("[Data]", "Response received from server: {}"), RecordUtils.toString(packetRespondedByServer.getResponse()));
                }

                if (LOG.isDebugEnabled()) {
                    LOG.debug("Reading reply sessionid:0x" + Long.toHexString(sessionId) + ", packet: " + packetRespondedByServer);
                }
            } finally {
                finishQueueingPacket(packetRespondedByServer);
            }
        }

        /**
         * Callback invoked by the ClientConnectionSocket once a connection has been
         * established.
         *
         * @param _negotiatedSessionTimeout
         * @param _sessionId
         * @param _sessionPasswd
         * @param isReadOnly
         * @throws IOException
         */
        public void onConnected(int _negotiatedSessionTimeout, long _sessionId, byte[] _sessionPasswd, boolean isReadOnly) throws Exception {
            negotiatedSessionTimeout = _negotiatedSessionTimeout;
            if (negotiatedSessionTimeout <= 0) {
                setClientConnectionState(ClientConnectionState.CLOSED);

                eventThread.queueEventOfDeath();

                String warnInfo;
                warnInfo = "Unable to reconnect to ZooKeeper service, session 0x"
                        + Long.toHexString(sessionId) + " has expired";
                LOG.warn(warnInfo);
                throw new SessionExpiredException(warnInfo);
            }
            if (!readOnly && isReadOnly) {
                LOG.error("Read/write client got connected to read-only server");
            }
            readTimeout = negotiatedSessionTimeout * 2 / 3;
            connectTimeout = negotiatedSessionTimeout / hostProvider.getNumberOfServerAddress();
            hostProvider.onConnected();
            sessionId = _sessionId;
            sessionPasswd = _sessionPasswd;
            setClientConnectionState((isReadOnly) ? ClientConnectionState.CONNECTED_READONLY : ClientConnectionState.CONNECTED);
            seenServerBefore |= !isReadOnly;
            LOG.info("Session establishment complete on server " + clientConnectionSocket.getRemoteSocketAddress() + ", sessionid = 0x" + Long.toHexString(sessionId) + ", negotiated timeout = " + negotiatedSessionTimeout + (isReadOnly ? " (READ-ONLY mode)" : ""));
            // todo v.y. watcher
//            Watcher.Event.KeeperState eventState = (isReadOnly) ? Watcher.Event.KeeperState.ConnectedReadOnly : Watcher.Event.KeeperState.SyncConnected;
        }

        /******************************* Heartbeat Processing *******************************/

        /**
         * This method is used to send a ping
         */
        private void sendPing() {
            lastPingSentNs = System.nanoTime();
            RequestHeader h = new RequestHeader(-2, OpCode.ping);
            queuePacket(h, null, null, null, null, null, null, null);
        }

        /**
         * This method is used to ping a server for read-only connection
         *
         * @throws ServerFoundException
         */
        private void pingServerForReadOnly() throws ServerFoundException {
            String result = null;
            InetSocketAddress serverAddress = hostProvider.getNextServerAddress(0);
            LOG.info("Checking server " + serverAddress + " for being r/w." + " Timeout " + pingServerTimeout);

            Socket socket = null;
            BufferedReader br = null;
            try {
                socket = new Socket(serverAddress.getHostName(), serverAddress.getPort());
                socket.setSoLinger(false, -1);
                socket.setSoTimeout(1000);
                socket.setTcpNoDelay(true);
                socket.getOutputStream().write("isro".getBytes());
                socket.getOutputStream().flush();
                socket.shutdownOutput();
                br = new BufferedReader(new InputStreamReader(socket.getInputStream()));
                result = br.readLine();
            } catch (ConnectException e) {
                // ignore, this just means server is not up
            } catch (IOException e) {
                // some unexpected error, warn about it
                LOG.warn("Exception while seeking for r/w server " + e.getMessage(), e);
            } finally {
                if (socket != null) {
                    try {
                        socket.close();
                    } catch (IOException e) {
                        LOG.warn("Unexpected exception", e);
                    }
                }
                if (br != null) {
                    try {
                        br.close();
                    } catch (IOException e) {
                        LOG.warn("Unexpected exception", e);
                    }
                }
            }

            if ("rw".equals(result)) {
                pingServerTimeout = minPingServerTimeout;
                // save the found address so that it's used during the next
                // connection attempt
                serverAddressToPing = serverAddress;
                throw new ServerFoundException("Majority server found at " + serverAddress.getHostName() + ":" + serverAddress.getPort());
            }
        }

        /******************************* Getter and Setter *******************************/

        public ClientConnectionSocket getClientConnectionSocket() {
            return clientConnectionSocket;
        }

        public ClientConnectionState getClientConnectionState() {
            return clientConnectionState;
        }
    }

    class EventThread extends UncaughtExceptionLogger {

        private final LinkedBlockingQueue<Object> waitingEvents = new LinkedBlockingQueue<>();
        private Object eventOfDeath = new Object();
        private volatile boolean wasKilled = false;
        private volatile boolean isRunning = false;

        EventThread() {
            super(makeThreadName("-EventThread"));
            setDaemon(true);
        }

        @Override
        public void run() {
            LOG.info(LogUtils.getMessage("Event thread starts running"));
            try {
                isRunning = true;
                while (true) {
                    Object event = waitingEvents.take();
                    if (event == eventOfDeath) {
                        wasKilled = true;
                    } else {
                        processEvent(event);
                    }
                    if (wasKilled) {
                        synchronized (waitingEvents) {
                            if (waitingEvents.isEmpty()) {
                                isRunning = false;
                                break;
                            }
                        }
                    }
                }
            } catch (InterruptedException e) {
                LOG.error("Event thread exiting due to interruption", e);
            }
            LOG.info(LogUtils.getMessage("Event thread completes running"));
        }

        /******************************* Packet Processing *******************************/

        /**
         * This method is used to queue the packet
         *
         * @param packet
         */
        public void queuePacket(Packet packet) {
            if (wasKilled) {
                synchronized (waitingEvents) {
                    if (isRunning) {
                        waitingEvents.add(packet);
                    }
                    else {
                        processEvent(packet);
                    }
                }
            } else {
                waitingEvents.add(packet);
            }
        }

        /**
         * This method is used to queue the event of death
         */
        public void queueEventOfDeath() {
            waitingEvents.add(eventOfDeath);
        }

        /******************************* Event Processing *******************************/

        /**
         *  This method is used to process event
         *  
         * @param event
         */
        private void processEvent(Object event) {
            try {
                if (event instanceof Packet) {
                    Packet p = (Packet) event;
                    int rc = 0;
                    String clientPath = p.getClientPath();
                    if (p.getResponseHeader().getErr() != 0) {
                        rc = p.getResponseHeader().getErr();
                    }
                    if (p.getCallback() == null) {
                        LOG.warn("Somehow a null cb got to EventThread!");
                    } else if (p.getResponse() instanceof ExistsResponse
                            || p.getResponse() instanceof SetDataResponse) {
                        StatCallback cb = (StatCallback) p.getCallback();
                        if (rc == 0) {
                            if (p.getResponse() instanceof ExistsResponse) {
                                cb.processResult(rc, clientPath, p.getContext(), ((ExistsResponse) p.getResponse()).getStat());
                            } else if (p.getResponse() instanceof SetDataResponse) {
                                cb.processResult(rc, clientPath, p.getContext(), ((SetDataResponse) p.getResponse()).getStat());
                            }
                        } else {
                            cb.processResult(rc, clientPath, p.getContext(), null);
                        }
                    } else if (p.getResponse() instanceof GetDataResponse) {
                        DataCallback cb = (DataCallback) p.getCallback();
                        GetDataResponse rsp = (GetDataResponse) p.getResponse();
                        if (rc == 0) {
                            cb.processResult(rc, clientPath, p.getContext(), rsp.getData(), rsp.getStat());
                        } else {
                            cb.processResult(rc, clientPath, p.getContext(), null, null);
                        }
                    } else if (p.getResponse() instanceof GetChildrenResponse) {
                        ChildrenCallback cb = (ChildrenCallback) p.getCallback();
                        GetChildrenResponse rsp = (GetChildrenResponse) p.getResponse();
                        if (rc == 0) {
                            cb.processResult(rc, clientPath, p.getContext(), rsp.getChildren());
                        } else {
                            cb.processResult(rc, clientPath, p.getContext(), null);
                        }
                    } else if (p.getResponse() instanceof GetChildren2Response) {
                        Children2Callback cb = (Children2Callback) p.getCallback();
                        GetChildren2Response rsp = (GetChildren2Response) p.getResponse();
                        if (rc == 0) {
                            cb.processResult(rc, clientPath, p.getContext(), rsp.getChildren(), rsp.getStat());
                        } else {
                            cb.processResult(rc, clientPath, p.getContext(), null, null);
                        }
                    } else if (p.getResponse() instanceof CreateResponse) {
                        StringCallback cb = (StringCallback) p.getCallback();
                        CreateResponse rsp = (CreateResponse) p.getResponse();
                        if (rc == 0) {
                            cb.processResult(rc, clientPath, p.getContext(), (chrootPath == null ? rsp.getPath() : rsp.getPath().substring(chrootPath.length())));
                        } else {
                            cb.processResult(rc, clientPath, p.getContext(), null);
                        }
                    } else if (p.getResponse() instanceof MultiResponse) {
                        MultiCallback cb = (MultiCallback) p.getCallback();
                        MultiResponse rsp = (MultiResponse) p.getResponse();
                        if (rc == 0) {
                            List<OpResult> results = rsp.getResultList();
                            int newRc = rc;
                            for (OpResult result : results) {
                                if (result instanceof OpResult.ErrorResult && HaimaException.Code.OK.intValue() != (newRc = ((OpResult.ErrorResult) result).getErr())) {
                                    break;
                                }
                            }
                            cb.processResult(newRc, clientPath, p.getContext(), results);
                        } else {
                            cb.processResult(rc, clientPath, p.getContext(), null);
                        }
                    } else if (p.getCallback() instanceof VoidCallback) {
                        VoidCallback cb = (VoidCallback) p.getCallback();
                        cb.processResult(rc, clientPath, p.getContext());
                    }
                }
            } catch (Throwable t) {
                LOG.error("Caught unexpected throwable", t);
            }
        }
    }
}
