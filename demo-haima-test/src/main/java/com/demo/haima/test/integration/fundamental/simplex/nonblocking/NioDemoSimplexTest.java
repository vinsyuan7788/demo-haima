package com.demo.haima.test.integration.fundamental.simplex.nonblocking;

import com.demo.haima.fundamental.client.simplex.nonblocking.DemoSimplexClient;
import com.demo.haima.fundamental.client.simplex.nonblocking.NioDemoSimplexClient;
import com.demo.haima.fundamental.server.simplex.nonblocking.DemoSimplexServer;
import com.demo.haima.fundamental.server.simplex.nonblocking.NioDemoSimplexServer;
import com.demo.haima.fundamental.utility.state.client.nonblocking.ClientState;
import com.demo.haima.fundamental.utility.state.server.nonblocking.ServerState;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.net.InetSocketAddress;
import java.util.Random;

/**
 * @author Vince Yuan
 * @date 2021/11/20
 */
public class NioDemoSimplexTest {

    private DemoSimplexServer server;
    private DemoSimplexClient client;

    private Object[] dataArrayToWrite = new Object[] { 12345, 54321L, "Hello NIO" };
    private Random random = new Random();

    @Before
    public void testStartServer() {
        server = new NioDemoSimplexServer(8686);
        server.startServer();
        while (server.getServerState() != ServerState.RUNNING) { }
        System.out.println("testStartServer is completed");
    }

    @Test
    public void test1() throws Exception {
        client = new NioDemoSimplexClient(new InetSocketAddress(8686));
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
        client = new NioDemoSimplexClient(new InetSocketAddress(8686));
        client.startClient();
        while (client.getClientState() != ClientState.RUNNING) { }

        long startTimeMillis = System.currentTimeMillis();
        for (int i = 0; i < 100; i++) {
            client.write(dataArrayToWrite[random.nextInt(dataArrayToWrite.length)]);
        }
        // ~10ms for 100 communications
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
