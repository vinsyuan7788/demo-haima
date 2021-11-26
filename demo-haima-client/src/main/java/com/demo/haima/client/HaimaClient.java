package com.demo.haima.client;

import com.demo.haima.client.connection.ClientConnection;
import com.demo.haima.client.connection.socket.ClientConnectionSocket;
import com.demo.haima.client.connection.socket.NioClientConnectionSocket;
import com.demo.haima.client.connection.state.ClientConnectionState;
import com.demo.haima.client.host.HostProvider;
import com.demo.haima.client.host.StaticHostProvider;
import com.demo.haima.client.parser.ConnectStringParser;
import com.demo.haima.common.definition.OpCode;
import com.demo.haima.common.exception.HaimaException;
import com.demo.haima.common.runtime.Environment;
import com.demo.haima.common.serdes.jute.request.SnowFlakeRequest;
import com.demo.haima.common.serdes.jute.request.header.RequestHeader;
import com.demo.haima.common.serdes.jute.response.SnowFlakeResponse;
import com.demo.haima.common.serdes.jute.response.header.ResponseHeader;
import com.demo.haima.common.utility.LogUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;

/**
 * This is the main class of Haima client library. To use a Haima
 * service, an application must first instantiate an object of Haima class.
 * All the iterations will be done by calling the methods of Haima class.
 * The methods of this class are thread-safe unless otherwise noted.
 * <p>
 * Once a connection to a server is established, a session ID is assigned to the
 * client. The client will send heart beats to the server periodically to keep
 * the session valid.
 * <p>
 * The application can call Haima client APIs through a client as long as the
 * session ID of the client remains valid.
 * <p>
 * If for some reason, the client fails to send heart beats to the server for a
 * prolonged period of time (exceeding the sessionTimeout value, for instance),
 * the server will expire the session, and the session ID will become invalid.
 * The client object will no longer be usable. To make Haima API calls, the
 * application must create a new client object.
 * <p>
 * If the Haima server the client currently connects to fails or otherwise
 * does not respond, the client will automatically try to connect to another
 * server before its session ID expires. If successful, the application can
 * continue to use the client.
 * <p>
 * The Haima API methods are either synchronous or asynchronous. Synchronous
 * methods blocks until the server has responded. Asynchronous methods just queue
 * the request for sending and return immediately. They take a callback object that
 * will be executed either on successful execution of the request or on error with
 * an appropriate return code (rc) indicating the error.
 *
 * @author Vince Yuan
 * @date 2021/11/10
 */
public class HaimaClient extends Client {

    private static final Logger LOG;

    private static final String HAIMA_CLIENT_CNXN_SOCKET = "haima.client.client.socket";

    static {
        // Keep these two lines together to keep the initialization order explicit
        LOG = LoggerFactory.getLogger(HaimaClient.class);
        Environment.logEnv("Client environment:", LOG);
    }

    private final ClientConnection clientConnection;

    /**
     * To create a Haima client object, the application needs to pass a
     * connection string containing a comma separated list of host:port pairs,
     * each corresponding to a Haima server.
     * <p>
     * Session establishment is asynchronous. This constructor will initiate
     * connection to the server and return immediately - potentially (usually)
     * before the session is fully established.
     * <p>
     * The instantiated Haima client object will pick an arbitrary server
     * from the connectString and attempt to connect to it. If establishment of
     * the connection fails, another server in the connect string will be tried
     * (the order is non-deterministic, as we random shuffle the list), until a
     * connection is established. The client will continue attempts until the
     * session is explicitly closed.
     * <p>
     * An optional "chroot" suffix may also be appended to the
     * connection string. This will run the client commands while interpreting
     * all paths relative to this root (similar to the unix chroot command).
     *
     * @param connectString
     *            comma separated host:port pairs, each corresponding to a zk
     *            server. e.g. "127.0.0.1:3000,127.0.0.1:3001,127.0.0.1:3002" If
     *            the optional chroot suffix is used the example would look
     *            like: "127.0.0.1:3000,127.0.0.1:3001,127.0.0.1:3002/app/a"
     *            where the client would be rooted at "/app/a" and all paths
     *            would be relative to this root - ie getting/setting/etc...
     *            "/foo/bar" would result in operations being run on
     *            "/app/a/foo/bar" (from the server perspective).
     * @param sessionTimeout
     *            session timeout in milliseconds
     * @throws Exception
     *            in cases of failure
     */
    public HaimaClient(String connectString, int sessionTimeout) throws Exception {
        this(connectString, sessionTimeout, false);
    }

    /**
     * To create a Haima client object, the application needs to pass a
     * connection string containing a comma separated list of host:port pairs,
     * each corresponding to a Haima server.
     * <p>
     * Session establishment is asynchronous. This constructor will initiate
     * connection to the server and return immediately - potentially (usually)
     * before the session is fully established.
     * <p>
     * The instantiated Haima client object will pick an arbitrary server
     * from the connectString and attempt to connect to it. If establishment of
     * the connection fails, another server in the connect string will be tried
     * (the order is non-deterministic, as we random shuffle the list), until a
     * connection is established. The client will continue attempts until the
     * session is explicitly closed.
     * <p>
     * An optional "chroot" suffix may also be appended to the
     * connection string. This will run the client commands while interpreting
     * all paths relative to this root (similar to the unix chroot command).
     *
     * @param port
     *            the local port for the client to bind
     * @param connectString
     *            comma separated host:port pairs, each corresponding to a zk
     *            server. e.g. "127.0.0.1:3000,127.0.0.1:3001,127.0.0.1:3002" If
     *            the optional chroot suffix is used the example would look
     *            like: "127.0.0.1:3000,127.0.0.1:3001,127.0.0.1:3002/app/a"
     *            where the client would be rooted at "/app/a" and all paths
     *            would be relative to this root - ie getting/setting/etc...
     *            "/foo/bar" would result in operations being run on
     *            "/app/a/foo/bar" (from the server perspective).
     * @param sessionTimeout
     *            session timeout in milliseconds
     * @throws Exception
     *            in cases of failure
     */
    public HaimaClient(int port, String connectString, int sessionTimeout) throws Exception {
        this(port, connectString, sessionTimeout, false);
    }

    /**
     * To create a Haima client object, the application needs to pass a
     * connection string containing a comma separated list of host:port pairs,
     * each corresponding to a Haima server.
     * <p>
     * Session establishment is asynchronous. This constructor will initiate
     * connection to the server and return immediately - potentially (usually)
     * before the session is fully established.
     * <p>
     * The instantiated Haima client object will pick an arbitrary server
     * from the connectString and attempt to connect to it. If establishment of
     * the connection fails, another server in the connect string will be tried
     * (the order is non-deterministic, as we random shuffle the list), until a
     * connection is established. The client will continue attempts until the
     * session is explicitly closed.
     * <p>
     * An optional "chroot" suffix may also be appended to the
     * connection string. This will run the client commands while interpreting
     * all paths relative to this root (similar to the unix chroot command).
     *
     * @param connectString
     *            comma separated host:port pairs, each corresponding to a zk
     *            server. e.g. "127.0.0.1:3000,127.0.0.1:3001,127.0.0.1:3002" If
     *            the optional chroot suffix is used the example would look
     *            like: "127.0.0.1:3000,127.0.0.1:3001,127.0.0.1:3002/app/a"
     *            where the client would be rooted at "/app/a" and all paths
     *            would be relative to this root - ie getting/setting/etc...
     *            "/foo/bar" would result in operations being run on
     *            "/app/a/foo/bar" (from the server perspective).
     * @param sessionTimeout
     *            session timeout in milliseconds
     * @param canBeReadOnly
     *            Whether the created client is allowed to go to
     *            read-only mode in case of partitioning. Read-only mode
     *            basically means that if the client can't find any majority
     *            servers but there's partitioned server it could reach, it
     *            connects to one in read-only mode, i.e. read requests are
     *            allowed while write requests are not. It continues seeking for
     *            majority in the background.
     * @throws Exception
     *            in cases of failure
     */
    public HaimaClient(String connectString, int sessionTimeout, boolean canBeReadOnly) throws Exception {
        LOG.info("Initiating client connection, connectString=" + connectString + " sessionTimeout=" + sessionTimeout);
        ConnectStringParser connectStringParser = new ConnectStringParser(connectString);
        HostProvider hostProvider = new StaticHostProvider(connectStringParser.getServerAddresses());
        LOG.info(LogUtils.getMessage("Host provider is instantiated"));
        ClientConnectionSocket clientConnectionSocket = getClientConnectionSocket();
        LOG.info(LogUtils.getMessage("Client connection socket is instantiated"));
        clientConnection = new ClientConnection(connectStringParser.getChrootPath(), hostProvider, sessionTimeout, this, clientConnectionSocket, canBeReadOnly);
        LOG.info(LogUtils.getMessage("Client connection is instantiated and set"));
        clientConnection.start();
        LOG.info(LogUtils.getMessage("Client connection is started"));
    }

    /**
     * To create a Haima client object, the application needs to pass a
     * connection string containing a comma separated list of host:port pairs,
     * each corresponding to a Haima server.
     * <p>
     * Session establishment is asynchronous. This constructor will initiate
     * connection to the server and return immediately - potentially (usually)
     * before the session is fully established.
     * <p>
     * The instantiated Haima client object will pick an arbitrary server
     * from the connectString and attempt to connect to it. If establishment of
     * the connection fails, another server in the connect string will be tried
     * (the order is non-deterministic, as we random shuffle the list), until a
     * connection is established. The client will continue attempts until the
     * session is explicitly closed.
     * <p>
     * An optional "chroot" suffix may also be appended to the
     * connection string. This will run the client commands while interpreting
     * all paths relative to this root (similar to the unix chroot command).
     *
     * @param port
     *            the local port for the client to bind
     * @param connectString
     *            comma separated host:port pairs, each corresponding to a zk
     *            server. e.g. "127.0.0.1:3000,127.0.0.1:3001,127.0.0.1:3002" If
     *            the optional chroot suffix is used the example would look
     *            like: "127.0.0.1:3000,127.0.0.1:3001,127.0.0.1:3002/app/a"
     *            where the client would be rooted at "/app/a" and all paths
     *            would be relative to this root - ie getting/setting/etc...
     *            "/foo/bar" would result in operations being run on
     *            "/app/a/foo/bar" (from the server perspective).
     * @param sessionTimeout
     *            session timeout in milliseconds
     * @param canBeReadOnly
     *            Whether the created client is allowed to go to
     *            read-only mode in case of partitioning. Read-only mode
     *            basically means that if the client can't find any majority
     *            servers but there's partitioned server it could reach, it
     *            connects to one in read-only mode, i.e. read requests are
     *            allowed while write requests are not. It continues seeking for
     *            majority in the background.
     * @throws Exception
     *            in cases of failure
     */
    public HaimaClient(int port, String connectString, int sessionTimeout, boolean canBeReadOnly) throws Exception {
        LOG.info("Initiating client connection, connectString=" + connectString + " sessionTimeout=" + sessionTimeout);
        ConnectStringParser connectStringParser = new ConnectStringParser(connectString);
        HostProvider hostProvider = new StaticHostProvider(connectStringParser.getServerAddresses());
        LOG.info(LogUtils.getMessage("Host provider is instantiated"));
        ClientConnectionSocket clientConnectionSocket = getClientConnectionSocket();
        clientConnectionSocket.setClientPort(port);
        LOG.info(LogUtils.getMessage("Client connection socket is instantiated"));
        clientConnection = new ClientConnection(connectStringParser.getChrootPath(), hostProvider, sessionTimeout, this, clientConnectionSocket, canBeReadOnly);
        LOG.info(LogUtils.getMessage("Client connection is instantiated and set"));
        clientConnection.start();
        LOG.info(LogUtils.getMessage("Client connection is started"));
    }

    @Override
    public ClientConnectionState getClientConnectionState() {
        return clientConnection.getClientConnectionState();
    }

    @Override
    public long getSessionId() {
        return clientConnection.getSessionId();
    }

    @Override
    public synchronized void close() {
        if (!clientConnection.getClientConnectionState().isAlive()) {
            if (LOG.isDebugEnabled()) {
                LOG.debug("Close called on already closed client");
            }
            return;
        }

        if (LOG.isDebugEnabled()) {
            LOG.debug("Closing session: 0x" + Long.toHexString(getSessionId()));
        }

        try {
            clientConnection.close();
        } catch (Exception e) {
            if (LOG.isDebugEnabled()) {
                LOG.debug("Ignoring unexpected exception during close", e);
            }
        }

        LOG.info("Session: 0x" + Long.toHexString(getSessionId()) + " closed");
    }

    @Override
    public long getSnowFlakeId(final int app) throws Exception {
        RequestHeader h = new RequestHeader();
        h.setType(OpCode.snowFlake);
        SnowFlakeRequest request = new SnowFlakeRequest();
        SnowFlakeResponse response = new SnowFlakeResponse();
        request.setApp(app);

        ResponseHeader r = clientConnection.submitRequest(h, request, response);
        if (r.getErr() != 0) {
            throw HaimaException.create(HaimaException.Code.get(r.getErr()), null);
        }
        return response.getSfId();
    }

    @Override
    public String toString() {
        ClientConnectionState state = getClientConnectionState();
        return ("State:" + state.toString()
                + (state.isConnected() ? " Negotiated session timeout:" + getNegotiatedSessionTimeout() + " " : " ")
                + clientConnection);
    }

    /****************************** Factory Method ******************************/

    /**
     * This method is used to get client connection socket instance
     *
     * @return
     * @throws IOException
     */
    private static ClientConnectionSocket getClientConnectionSocket() throws IOException {
        String clientConnectionSocketName = getClientConnectionSocketName();
        try {
            return (ClientConnectionSocket) Class.forName(clientConnectionSocketName).newInstance();
        } catch (Exception e) {
            IOException ioe = new IOException("Couldn't instantiate " + clientConnectionSocketName);
            ioe.initCause(e);
            throw ioe;
        }
    }

    /**
     * This method is used to get client connection socket name
     *
     * @return
     */
    private static String getClientConnectionSocketName() {
        String clientCnxnSocketName = System.getProperty(HAIMA_CLIENT_CNXN_SOCKET);
        if (clientCnxnSocketName == null) {
            clientCnxnSocketName = NioClientConnectionSocket.class.getName();
        }
        return clientCnxnSocketName;
    }

    /****************************** Getter and Setter ******************************/

    public int getNegotiatedSessionTimeout() {
        return clientConnection.getNegotiatedSessionTimeout();
    }
}
