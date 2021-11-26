package com.demo.haima.test.integration.haima;

import com.demo.haima.client.Client;
import com.demo.haima.client.HaimaClient;
import com.demo.haima.client.connection.state.ClientConnectionState;
import com.demo.haima.common.utility.LogUtils;
import com.demo.haima.common.utility.StringUtils;
import com.demo.haima.server.HaimaServer;
import com.demo.haima.server.Server;
import com.demo.haima.server.connection.factory.ServerConnectionFactory;
import com.demo.haima.server.snowflake.factory.SnowFlakeIdWorkerFactory;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.SocketTimeoutException;

/**
 * This class is used to test Haima server and client
 *
 * @author Vince Yuan
 * @date 2021/11/14
 */
public class HaimaOneTimeRunTest {

    private static final Logger LOG = LoggerFactory.getLogger(HaimaOneTimeRunTest.class);

    private class ServerConfig {
        private static final String HOST = "localhost";
        private static final int PORT = 8585;
        private static final int MAX_NUMBER_OF_CLIENT_CONNECTION = 60;
        private static final int TICK_TIME = 3000;
        private static final int MIN_SESSION_TIMEOUT = -1;
        private static final int MAX_SESSION_TIMEOUT = -1;
    }

    private class ClientConfig {
        private static final int PORT = 8484;
        private static final String CONNECT_STRING = ServerConfig.HOST + ":" + ServerConfig.PORT;
        private static final int SESSION_TIMEOUT = 50000;
        private static final int DEFAULT_SOCKET_TIMEOUT = 5000;
    }

    private ServerConnectionFactory serverConnectionFactory;
    private Server server;
    private Client client;

    @Before
    public void testStartServer() throws Exception {
        SnowFlakeIdWorkerFactory.init(5, 10, 2);
        server = new HaimaServer();
        server.setTickTime(ServerConfig.TICK_TIME);
        server.setMinSessionTimeout(ServerConfig.MIN_SESSION_TIMEOUT);
        server.setMaxSessionTimeout(ServerConfig.MAX_SESSION_TIMEOUT);
        serverConnectionFactory = ServerConnectionFactory.createFactory();
        serverConnectionFactory.configure(new InetSocketAddress(ServerConfig.PORT), ServerConfig.MAX_NUMBER_OF_CLIENT_CONNECTION);
        serverConnectionFactory.startup(server);
    }

    @Test
    public void testConnectClient() throws Exception {

        client = new HaimaClient(ClientConfig.CONNECT_STRING, ClientConfig.SESSION_TIMEOUT);
        while (client.getClientConnectionState() != ClientConnectionState.CONNECTED) {
            Thread.sleep(1000);
        };

        long sessionId = client.getSessionId();
        System.out.println("Established Session with SessionId:" + sessionId);

        client.close();
        LOG.info(LogUtils.getMessage("testConnectClient is completed"));
    }

    @Test
    public void testSnowFlake() throws Exception {

        client = new HaimaClient(ClientConfig.PORT, ClientConfig.CONNECT_STRING, ClientConfig.SESSION_TIMEOUT);
        while (client.getClientConnectionState() != ClientConnectionState.CONNECTED) {
            Thread.sleep(1000);
        };

        long sessionId = client.getSessionId();
        long startTimeMillis = System.currentTimeMillis();
        for (int s = 0; s < 100; s++) {
            long sfId = client.getSnowFlakeId(3);
            System.out.println(sfId);
        }
        long durationMillis = System.currentTimeMillis() - startTimeMillis;
        System.out.println("Server test with sessionId " + sessionId + " success | used time: " + durationMillis + "ms");

        client.close();
        LOG.info(LogUtils.getMessage("testSnowFlake is completed"));
    }

    @Test
    public void test4LetterWord() throws Exception {

        long startTimeMillis = System.currentTimeMillis();
        for (int i = 0; i < 10000; i++) {
            String result = send4LetterWord(ServerConfig.HOST, ServerConfig.PORT, "stat", ClientConfig.DEFAULT_SOCKET_TIMEOUT);
            System.out.println("Stats command result: " + result);
            Assert.assertTrue(StringUtils.isNotBlank(result));
        }
        long durationMillis = System.currentTimeMillis() - startTimeMillis;

        System.out.println("Server test success used time: " + durationMillis + "ms");
        LOG.info(LogUtils.getMessage("test4LetterWord is completed"));
    }

    @After
    public void testShutdownServer() throws Exception {
        serverConnectionFactory.shutdown();
        if (server.canShutdown()) {
            server.shutdown(true);
        }
    }

    /**
     * This method is used to send the 4letterWord command to the server
     *
     * @param host the destination host
     * @param port the destination port
     * @param cmd the 4letterWord command
     * @param socketTimeout in milliseconds, maximum time to wait while connecting/reading data
     * @return
     * @throws Exception
     */
    private String send4LetterWord(String host, int port, String cmd, int socketTimeout) throws Exception {
        LOG.info("connecting to " + host + " " + port);
        Socket socket = new Socket();
        InetSocketAddress serverAddress = host != null ?
                new InetSocketAddress(host, port) :
                new InetSocketAddress(InetAddress.getByName(null), port);
        BufferedReader reader = null;
        try {
            socket.setSoTimeout(socketTimeout);
            socket.connect(serverAddress, socketTimeout);
            OutputStream outputStream = socket.getOutputStream();
            outputStream.write(cmd.getBytes());
            outputStream.flush();
            // this replicates NC - close the output stream before reading
            socket.shutdownOutput();

            reader = new BufferedReader(
                    new InputStreamReader(socket.getInputStream()));
            StringBuilder sb = new StringBuilder();
            String line;
            while((line = reader.readLine()) != null) {
                sb.append(line + "\n");
            }
            return sb.toString();
        } catch (SocketTimeoutException e) {
            throw new IOException("Exception while executing four letter word: " + cmd, e);
        } finally {
            socket.close();
            if (reader != null) {
                reader.close();
            }
        }
    }
}
