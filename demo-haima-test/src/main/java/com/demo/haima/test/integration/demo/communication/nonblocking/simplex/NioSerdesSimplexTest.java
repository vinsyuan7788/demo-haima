package com.demo.haima.test.integration.demo.communication.nonblocking.simplex;

import com.demo.haima.test.integration.demo.communication.nonblocking.simplex.client.NioSerdesSimplexClient;
import com.demo.haima.test.integration.demo.communication.nonblocking.simplex.client.SerdesSimplexClient;
import com.demo.haima.test.integration.demo.communication.nonblocking.simplex.server.NioSerdesSimplexServer;
import com.demo.haima.test.integration.demo.communication.nonblocking.simplex.server.SerdesSimplexServer;
import com.demo.haima.test.integration.demo.utils.state.client.nio.ClientState;
import com.demo.haima.test.integration.demo.utils.state.server.nio.ServerState;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.net.InetSocketAddress;
import java.util.Random;

/**
 * @author Vince Yuan
 * @date 2021/11/20
 */
public class NioSerdesSimplexTest {

    private SerdesSimplexServer server;
    private SerdesSimplexClient client;

    private Object[] dataArrayToWrite = new Object[] { 12345, 54321L, "Hello NIO" };
    private Random random = new Random();

    @Before
    public void testStartServer() {
        server = new NioSerdesSimplexServer(8686);
        server.startServer();
        while (server.getServerState() != ServerState.RUNNING) { }
        System.out.println("testStartServer is completed");
    }

    @Test
    public void test1() throws Exception {
        client = new NioSerdesSimplexClient(new InetSocketAddress(8686));
        client.startClient();
        while (client.getClientState() != ClientState.RUNNING) { }

        long startTimeMillis = System.currentTimeMillis();
        client.write(dataArrayToWrite[random.nextInt(dataArrayToWrite.length)]);
        System.out.println("Time elapsed for one-time communication: " + (System.currentTimeMillis() - startTimeMillis) + "ms");

        client.closeClient();
        System.out.println("testClientConnection is completed");
    }

    @Test
    public void test2() throws Exception {
        client = new NioSerdesSimplexClient(new InetSocketAddress(8686));
        client.startClient();
        while (client.getClientState() != ClientState.RUNNING) { }

        long startTimeMillis = System.currentTimeMillis();
        for (int i = 0; i < 100; i++) {
            client.write(dataArrayToWrite[random.nextInt(dataArrayToWrite.length)]);
        }
        // ~1ms for 100 communications
        System.out.println("Time elapsed for highly-intensive communication: " + (System.currentTimeMillis() - startTimeMillis) + "ms");

        client.closeClient();
        System.out.println("testIntensiveCommunication is completed");
    }

    @After
    public void testCloseServer() throws Exception {
        Thread.sleep(1000);
        server.closeServer();
        System.out.println("testCloseServer is completed");
    }
}
