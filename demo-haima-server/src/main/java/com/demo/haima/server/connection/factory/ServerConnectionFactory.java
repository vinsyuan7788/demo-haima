package com.demo.haima.server.connection.factory;

import com.demo.haima.server.Server;
import com.demo.haima.server.connection.NioServerConnection;
import com.demo.haima.server.connection.ServerConnection;
import com.demo.haima.server.jmx.bean.ConnectionMBean;
import com.demo.haima.server.jmx.registry.MBeanRegistry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.util.HashSet;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Corresponds to ClientConnection in the client-side
 *
 * @author Vince Yuan
 * @date 2021/11/9
 */
public abstract class ServerConnectionFactory {

    private static final Logger LOG = LoggerFactory.getLogger(ServerConnectionFactory.class);

    private static final String HAIMA_SERVER_CONNECTION_FACTORY = "haima.server.connection.factory";

    public static final ByteBuffer CLOSE_CONNECTION_BUFFER = ByteBuffer.allocate(0);

    /**
     * Default port for the server to bind
     */
    protected final int serverPort = 8585;
    /**
     * The server instance maintained by factory
     */
    protected Server server;
    /**
     * This is used to track each connection from whatever client for unified management.
     * Once a connection is accepted, it will be added here.
     * Once a connection is closed, it will be removed here.
     */
    protected HashSet<NioServerConnection> clientConnectionSet = new HashSet<>();

    /**
     * This is used to track each connection's MBean for JMX management
     * Once a connection is registered to JMX, the corresponding MBean will be put here with the connection as its key.
     * Once a connection is unregistered to JMX, the corresponding MBean will be removed by its key (i.e., the connection)
     */
    private final Map<ServerConnection, ConnectionMBean> clientConnectionAndMBean = new ConcurrentHashMap<>();

    /******************************* Factory Method *******************************/

    /**
     * This method is used to create a server connection factory instance
     *
     * @return
     */
    public static ServerConnectionFactory createFactory() throws Exception {
        String serverConnectionFactoryName = getServerConnectionFactoryName();
        return (ServerConnectionFactory) Class.forName(serverConnectionFactoryName).newInstance();
    }

    /**
     * This method is used to get the name of server connection factory
     *
     * @return
     */
    private static String getServerConnectionFactoryName() {
        String serverConnectionFactoryName = System.getProperty(HAIMA_SERVER_CONNECTION_FACTORY);
        if (serverConnectionFactoryName == null) {
            serverConnectionFactoryName = NioServerConnectionFactory.class.getName();
        }
        return serverConnectionFactoryName;
    }

    /******************************* Getter and Setter *******************************/

    public HashSet<NioServerConnection> getClientConnectionSet() {
        return clientConnectionSet;
    }

    /******************************* JMX Management *******************************/

    /**
     * This method is used to register connection to JMX management
     *
     * @param serverConnection
     */
    public void registerConnection(ServerConnection serverConnection) {
        if (server != null) {
            ConnectionMBean connectionMBean = new ConnectionMBean(serverConnection, server);
            try {
                MBeanRegistry.getInstance().register(connectionMBean, server.getServerMBean());
                clientConnectionAndMBean.put(serverConnection, connectionMBean);
            } catch (Exception e) {
                LOG.warn("Could not register connection", e);
            }
        }
    }

    /**
     * This method is used to unregister connection from JMX management
     *
     * @param serverConnection
     */
    public void unregisterConnection(ServerConnection serverConnection) {
        ConnectionMBean connectionMBean = clientConnectionAndMBean.remove(serverConnection);
        if (connectionMBean != null){
            MBeanRegistry.getInstance().unRegister(connectionMBean);
        }
    }

    /******************************* Setup and Shutdown *******************************/

    /**
     * This method is used to configure the factory after it has been instantiated
     *
     * @param serverAddress
     * @param maxNumberOfClientConnection
     * @throws Exception
     */
    public abstract void configure(InetSocketAddress serverAddress, Integer maxNumberOfClientConnection) throws Exception;

    /**
     * This method is used to start up the server
     *
     * @param server
     * @throws Exception
     */
    public abstract void startup(Server server);

    /**
     * This method is used to shut down the server
     *
     * @throws Exception
     */
    public abstract void shutdown() throws Exception;

    /******************************* Getter and Setter *******************************/

    /**
     * This method is used to get the port of the server
     *
     * @return
     */
    public abstract int getServerPort();

    /******************************* Session Processing *******************************/

    /**
     * This method is used to close a specific session
     *
     * @param sessionId
     */
    public abstract void closeSession(long sessionId);
}
