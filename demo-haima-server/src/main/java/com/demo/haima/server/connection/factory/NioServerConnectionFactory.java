package com.demo.haima.server.connection.factory;

import com.demo.haima.common.constant.CommonConstant;
import com.demo.haima.common.exception.logger.UncaughtExceptionLogger;
import com.demo.haima.common.utility.CollectionUtils;
import com.demo.haima.common.utility.LogUtils;
import com.demo.haima.common.utility.ObjectUtils;
import com.demo.haima.server.HaimaServer;
import com.demo.haima.server.Server;
import com.demo.haima.server.connection.NioServerConnection;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * @author Vince Yuan
 * @date 2021/11/9
 */
public class NioServerConnectionFactory extends ServerConnectionFactory implements Runnable {

    private static final Logger LOG = LoggerFactory.getLogger(NioServerConnectionFactory.class);

    static {
        /**
         * this is to avoid the jvm bug:
         * NullPointerException in Selector.open()
         * http://bugs.sun.com/view_bug.do?bug_id=6427854
         */
        try {
            Selector.open().close();
        } catch (Exception e) {
            LOG.error("Selector failed to open", e);
        }
    }

    private int maxNumberOfClientConnection = 60;

    private Thread thread;
    private final Selector selector = Selector.open();
    private ServerSocketChannel serverSocketChannel;

    /**
     * This is used to track the number of connection from each client
     * Each client has its maximum limit number of connection to the server,
     * once the number of connection of client reaches this limit, the connection will be skipped processing.
     * See {@link NioServerConnectionFactory#run} method
     */
    private final HashMap<InetAddress, Set<NioServerConnection>> clientIpAddressAndConnectionsMap = new HashMap<>();
    /**
     * We use this buffer to do efficient socket I/O. Since there is a single
     * sender thread per NIOServerConnection instance, we can use a member variable to
     * only allocate it once.
     */
    private final ByteBuffer directBuffer = ByteBuffer.allocateDirect(64 * 1024);

    /**
     * Construct a new server connection factory which will accept an unlimited number
     * of concurrent connections from each client (up to the file descriptor
     * limits of the operating system). startup(zks) must be called subsequently.
     */
    public NioServerConnectionFactory() throws Exception {
        LOG.info(LogUtils.getMessage("Selector is instantiated"));
        // information logging here
        Set<SelectionKey> keys = selector.keys();
        LOG.info(LogUtils.getMessage("Number of registration of a server socket channel with a selector: {}"), keys.size());
    }

    @Override
    public void configure(InetSocketAddress serverAddress, Integer maxNumberOfClientConnection) throws Exception {

        if (serverAddress == null) {
            serverAddress = new InetSocketAddress(serverPort);
        }
        if (maxNumberOfClientConnection != null) {
            this.maxNumberOfClientConnection = maxNumberOfClientConnection;
        }

        thread = new UncaughtExceptionLogger(this, "NIOServerCxn.Factory:" + serverAddress);
        LOG.info(LogUtils.getMessage("NIO server connection factory thread is instantiated"));
        thread.setDaemon(true);

        serverSocketChannel = ServerSocketChannel.open();
        LOG.info(LogUtils.getMessage("Server socket channel is opened"));
        serverSocketChannel.socket().setReuseAddress(true);
        serverSocketChannel.socket().bind(serverAddress);
        LOG.info(LogUtils.getMessage("Server socket channel is bound to server address: {}"), serverAddress);
        serverSocketChannel.configureBlocking(false);
        LOG.info(LogUtils.getMessage("Server socket channel is non-blocking: {}"), true);
        serverSocketChannel.register(selector, SelectionKey.OP_ACCEPT);

        // information logging here
        LOG.info(LogUtils.getMessage("Server socket channel is registered with selector: {}"), selector);
        Set<SelectionKey> keys = selector.keys();
        LOG.info(LogUtils.getMessage("Number of registration of a server socket channel with a selector: {}"), keys.size());
    }

    @Override
    public void startup(Server server) {
        start();
        setServer(server);
        server.startData();
        server.startup();
    }

    @Override
    public void shutdown() throws Exception {

        serverSocketChannel.close();
        closeAll();
        thread.interrupt();
        thread.join();

        selector.close();

        if (server != null) {
            server.shutdown(false);
        }
    }

    @Override
    public int getServerPort() {
        return serverSocketChannel.socket().getLocalPort();
    }

    @Override
    public synchronized void closeSession(long sessionId) {
        selector.wakeup();
        closeSessionWithoutWakeup(sessionId);
    }

    /**
     * This method is used to process.
     * In this method, if using:
     * <blockquote><pre>
     * selector.selectNow();
     * ...
     * </pre></blockquote>
     * or
     * <blockquote><pre>
     * int selectNum = selector.select(millis);
     * if (selectNum < 0) {
     *     continue;
     * }
     * ...
     * </pre></blockquote>
     * Some sockets coming from the client may get loss (or missed, skipped processing) by the server,
     * and thus cause the client to keep waiting until the connection of client-side is loss due to
     * session timeout of client-side.
     * Same scenario happens in the client as well if the client-side uses the codes above-mentioned.
     * Hence, here uses "selector.select(millis)" and leaves the "selector.selectedKeys()" to tell if
     * there is any socket that really needs to be processed (to ensure the server won't miss any socket
     * channel that is ready to be processed)
     */
    @Override
    public void run() {

        LOG.info(LogUtils.getMessage("NIO server connection factory thread starts running"));

        boolean isServerSocketClosed = serverSocketChannel.socket().isClosed();
        while (!isServerSocketClosed) {
            try {
                selector.select(CommonConstant.ONE_THOUSAND);
                Set<SelectionKey> selectedKeySet;
                synchronized (this) {
                    selectedKeySet = selector.selectedKeys();
                }

                List<SelectionKey> selectedKeyList = new ArrayList<>(selectedKeySet);
                Collections.shuffle(selectedKeyList);

                for (SelectionKey selectedKey : selectedKeyList) {
                    if (selectedKey.isAcceptable()) {
                        SocketChannel acceptedSocketChannel = ((ServerSocketChannel) selectedKey.channel()).accept();
                        if (acceptedSocketChannel == null) {
                            continue;
                        }
                        InetAddress clientIpAddress = acceptedSocketChannel.socket().getInetAddress();
                        int numberOfClientConnection = getNumberOfClientConnection(clientIpAddress);

                        SocketAddress clientSocketAddress = acceptedSocketChannel.socket().getRemoteSocketAddress();
                        SocketAddress serverSocketAddress = acceptedSocketChannel.socket().getLocalSocketAddress();
                        /*
                         * If the number of connection from a client reaches its threshold, it will be closed directly,
                         * otherwise, it can be accepted and further processed
                         */
                        if (maxNumberOfClientConnection > 0 && numberOfClientConnection > maxNumberOfClientConnection) {
                            LOG.warn("Too many connections from {} to {}, since max is {}", clientSocketAddress, serverSocketAddress, maxNumberOfClientConnection);
                            acceptedSocketChannel.close();
                        } else {
                            LOG.info("Accepted socket connection from {} to {}", clientSocketAddress, serverSocketAddress);
                            acceptedSocketChannel.configureBlocking(false);
                            SelectionKey registeredKey = acceptedSocketChannel.register(selector, SelectionKey.OP_READ);
                            NioServerConnection connection = wrapClientConnection(acceptedSocketChannel, registeredKey);
                            registeredKey.attach(connection);
                            addClientConnection(connection);
                        }
                    } else if (selectedKey.isReadable() || selectedKey.isWritable()) {
                        NioServerConnection connection = (NioServerConnection) selectedKey.attachment();
                        connection.doIO(selectedKey);
                    } else {
                        LOG.info("Unexpected ops in select {}", selectedKey.readyOps());
                    }
                }
                selectedKeyList.clear();
                isServerSocketClosed = serverSocketChannel.socket().isClosed();
            } catch (Exception e) {
                // todo v.y. ex
//                e.printStackTrace();
            }
        }
        closeAll();
        LOG.info(LogUtils.getMessage("NIO server connection factory thread completes running"));
    }

    /******************************* Getter and Setter *******************************/

    public Server getServer() {
        return server;
    }

    public Map<InetAddress, Set<NioServerConnection>> getClientIpAddressAndConnectionsMap() {
        return clientIpAddressAndConnectionsMap;
    }

    public ByteBuffer getDirectBuffer() {
        return directBuffer;
    }

    /******************************* Setup and Shutdown *******************************/

    /**
     * This method is used to start running current factory thread
     */
    private void start() {
        /**
         *  ensure uncaughtExceptionCatcher thread is started once and only once
         *  -- when uncaughtExceptionCatcher starts, the run method within this class will be invoked
         *     -- since this instance is passed to uncaughtExceptionCatcher during the instantiation of UncaughtExceptionCatcher
         */
        if (thread.getState() == Thread.State.NEW) {
            thread.start();
            LOG.info(LogUtils.getMessage("NIO server connection factory thread is started"));
        }
    }

    /**
     * This method is used to set the server
     *
     * @param server
     */
    private void setServer(Server server) {
        this.server = server;
        LOG.info(LogUtils.getMessage("Server is set"));
        if (server != null) {
            server.setServerConnectionFactory(this);
        }
    }

    /**
     * This method is used to close all client connections managed by server
     */
    private void closeAll() {

        selector.wakeup();

        HashSet<NioServerConnection> clientConnectionSet;
        synchronized (this.clientConnectionSet) {
            clientConnectionSet = (HashSet<NioServerConnection>) this.clientConnectionSet.clone();
        }
        // got to clear all the connections that we have in the selector
        for (NioServerConnection connection: clientConnectionSet) {
            try {
                // don't hold this.clientConnectionSet lock as deadlock may occur
                connection.close();
            } catch (Exception e) {
                LOG.warn("Ignoring exception closing connection sessionid 0x"
                        + Long.toHexString(connection.getSessionId()), e);
            }
        }
    }

    /**
     * This method is used to close the connection associated the specific session
     *
     * @param sessionId
     */
    private void closeSessionWithoutWakeup(long sessionId) {

        HashSet<NioServerConnection> clientConnectionSet;
        synchronized (this.clientConnectionSet) {
            clientConnectionSet = (HashSet<NioServerConnection>)this.clientConnectionSet.clone();
        }

        for (NioServerConnection connection : clientConnectionSet) {
            if (connection.getSessionId() == sessionId) {
                try {
                    connection.close();
                } catch (Exception e) {
                    LOG.warn("exception during session close", e);
                }
                break;
            }
        }
    }

    /******************************* Request Processing *******************************/

    private NioServerConnection wrapClientConnection(SocketChannel socketChannel, SelectionKey selectionKey) throws Exception {
        return new NioServerConnection((HaimaServer) server, socketChannel, selectionKey, this);
    }

    private int getNumberOfClientConnection(InetAddress clientIpAddress) {
        // The clientIpAddressAndConnectionsMap lock covers both the map, and its contents
        // (that is, the connection sets shouldn't be modified outside of
        // this lock)
        synchronized (clientIpAddressAndConnectionsMap) {
            Set<NioServerConnection> clientConnections = clientIpAddressAndConnectionsMap.get(clientIpAddress);
            return CollectionUtils.isEmpty(clientConnections) ? 0 : clientConnections.size();
        }
    }

    private void addClientConnection(NioServerConnection connection) {
        synchronized (connection) {
            clientConnectionSet.add(connection);
            synchronized (clientIpAddressAndConnectionsMap){
                InetAddress clientIpAddress = connection.getSocketChannel().socket().getInetAddress();
                Set<NioServerConnection> clientConnections = clientIpAddressAndConnectionsMap.get(clientIpAddress);
                if (ObjectUtils.isNull(clientConnections)) {
                    // in general we will see 1 connection from each
                    // host, setting the initial cap to 2 allows us
                    // to minimize mem usage in the common case
                    // of 1 entry --  we need to set the initial cap
                    // to 2 to avoid rehash when the first entry is added
                    clientConnections = new HashSet<>(2);
                    clientConnections.add(connection);
                    clientIpAddressAndConnectionsMap.put(clientIpAddress, clientConnections);
                } else {
                    clientConnections.add(connection);
                }
            }
        }
    }
}
