package com.demo.haima.test.integration.haima;

import com.demo.haima.client.Client;
import com.demo.haima.client.HaimaClient;
import com.demo.haima.client.connection.state.ClientConnectionState;
import com.demo.haima.common.utility.LogUtils;
import com.demo.haima.server.HaimaServer;
import com.demo.haima.server.Server;
import com.demo.haima.server.connection.factory.ServerConnectionFactory;
import com.demo.haima.server.shutdown.HaimaServerShutdownHandler;
import com.demo.haima.server.snowflake.factory.SnowFlakeIdWorkerFactory;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.InetSocketAddress;
import java.util.concurrent.CountDownLatch;

/**
 * This class is highly NOT recommended to run, unless it can be ensured that
 * the Haima server can be shutdown through the shutdown latch, or the port of
 * server is not in-use when the server is shutdown in a graceful or forceful way
 *
 * @author Vince Yuan
 * @date 2021/11/12
 */
public class HaimaKeepRunningTest {

    private static final Logger LOG = LoggerFactory.getLogger(HaimaKeepRunningTest.class);

    private class ServerConfig {
        private static final String ADDRESS = "localhost";
        private static final int PORT = 8585;
        private static final int MAX_NUMBER_OF_CLIENT_CONNECTION = 60;
        private static final int TICK_TIME = 3000;
        private static final int MIN_SESSION_TIMEOUT = -1;
        private static final int MAX_SESSION_TIMEOUT = -1;
    }

    private class ClientConfig {
        private static final int PORT = 8484;
        private static final String CONNECT_STRING = ServerConfig.ADDRESS + ":" + ServerConfig.PORT;
        private static final int SESSION_TIMEOUT = 3000;
    }

    private CountDownLatch shutdownLatch;
    private ServerConnectionFactory serverConnectionFactory;
    private Server server;
    private Client client;

    @Before
    public void testStartServer() throws Exception {
        SnowFlakeIdWorkerFactory.init(5, 10, 2);
        shutdownLatch = new CountDownLatch(1);
        server = new HaimaServer();
        server.registerServerShutdownHandler(new HaimaServerShutdownHandler(shutdownLatch));
        server.setTickTime(ServerConfig.TICK_TIME);
        server.setMinSessionTimeout(ServerConfig.MIN_SESSION_TIMEOUT);
        server.setMaxSessionTimeout(ServerConfig.MAX_SESSION_TIMEOUT);
        serverConnectionFactory = ServerConnectionFactory.createFactory();
        serverConnectionFactory.configure(new InetSocketAddress(ServerConfig.ADDRESS, ServerConfig.PORT), ServerConfig.MAX_NUMBER_OF_CLIENT_CONNECTION);
        serverConnectionFactory.startup(server);
    }

    @Test
    public void testStartClient() throws Exception {
        client = new HaimaClient(ClientConfig.PORT, ClientConfig.CONNECT_STRING, ClientConfig.SESSION_TIMEOUT);
        while (client.getClientConnectionState() != ClientConnectionState.CONNECTED) {
            Thread.sleep(1000);
        };
        long sessionId = client.getSessionId();
        System.out.println("Established Session with SessionId:" + sessionId);

        client.close();
        LOG.info(LogUtils.getMessage("testStartClient is completed"));
    }

    @After
    public void testKeepServer() throws Exception {
        shutdownLatch.await();
        serverConnectionFactory.shutdown();
        if (server.canShutdown()) {
            server.shutdown(true);
        }
    }
}
