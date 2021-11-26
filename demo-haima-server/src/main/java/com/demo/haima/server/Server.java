package com.demo.haima.server;

import com.demo.haima.server.connection.ServerConnection;
import com.demo.haima.server.connection.factory.ServerConnectionFactory;
import com.demo.haima.server.jmx.bean.ServerMBean;
import com.demo.haima.server.shutdown.HaimaServerShutdownHandler;
import com.demo.haima.server.shutdown.ServerShutdownHandler;
import com.demo.haima.server.state.ServerState;

import java.nio.ByteBuffer;

/**
 * @author Vince Yuan
 * @date 2021/11/9
 */
public abstract class Server {

    protected ServerMBean serverMBean;

    /******************************* Getter and Setter *******************************/

    public ServerMBean getServerMBean() {
        return serverMBean;
    }

    /******************************* Setup and Shutdown *******************************/

    /**
     * This method is used to set the tick-time
     *
     * @param tickTime
     */
    public abstract void setTickTime(int tickTime);

    /**
     * This method is used to set the minimum session timeout
     *
     * @param minSessionTimeout
     */
    public abstract void setMinSessionTimeout(int minSessionTimeout);

    /**
     * This method is used to set the maximum session timeout
     *
     * @param maxSessionTimeout
     */
    public abstract void setMaxSessionTimeout(int maxSessionTimeout);

    /**
     * This method is used to register the ServerShutdownHandler to get
     * server's error or shutdown state change notifications. For example,
     * {@link HaimaServerShutdownHandler#handle(ServerState)} will be called for
     * every server state changes {@link HaimaServer#setServerState(ServerState)}.
     *
     * @param serverShutdownHandler shutdown handler
     */
    public abstract void registerServerShutdownHandler(ServerShutdownHandler serverShutdownHandler);

    /**
     * This method can be used while shutting down the server to see whether the server
     * is already shutdown or not.
     *
     * @return true if the server is running or server hits an error, false otherwise.
     */
    public abstract boolean canShutdown();

    /**
     * This method is used to shut down the server instance
     *
     * @param fullyShutdown true if another server using the same database will not replace this one in the same process
     */
    public abstract void shutdown(boolean fullyShutdown);

    /**
     * This method is used to set the server connection factory that starts up current server
     *
     * @param serverConnectionFactory
     */
    public abstract void setServerConnectionFactory(ServerConnectionFactory serverConnectionFactory);

    /**
     * This method is used to get the server connection factory that starts up current server
     *
     * @return
     */
    public abstract ServerConnectionFactory getServerConnectionFactory();

    /**
     * This method is used to start the database and load the data
     */
    public abstract void startData();

    /**
     * This method is used to start up the server
     */
    public abstract void startup();

    /******************************* Getter and Setter *******************************/

    /**
     * This method is used to tell if the server is running
     *
     * @return
     */
    public abstract boolean isRunning();

    /**
     * This method is used to get the port of the server
     *
     * @return
     */
    public abstract int getServerPort();

    /******************************* Request Processing *******************************/

    /**
     * This method is used to process connect request.
     * [Process]
     *
     * @param serverConnection
     * @param incomingBuffer
     * @throws Exception
     */
    public abstract void processConnectRequest(ServerConnection serverConnection, ByteBuffer incomingBuffer) throws Exception;

    /**
     * This method is used to process packet.
     * [Process]
     *
     * @param serverConnection
     * @param incomingBuffer
     * @throws Exception
     */
    public abstract void processPacket(ServerConnection serverConnection, ByteBuffer incomingBuffer) throws Exception;
}
