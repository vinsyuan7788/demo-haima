package com.demo.haima.test.integration.demo.communication.nonblocking.simplex;

import com.demo.haima.test.integration.demo.communication.nonblocking.simplex.client.BaseSimplexClient;
import com.demo.haima.test.integration.demo.communication.nonblocking.simplex.client.NioPacketSimplexClient;
import com.demo.haima.test.integration.demo.communication.nonblocking.simplex.client.PacketSimplexClient;
import com.demo.haima.test.integration.demo.communication.nonblocking.simplex.server.NioPacketSimplexServer;
import com.demo.haima.test.integration.demo.communication.nonblocking.simplex.server.PacketSimplexServer;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.net.InetSocketAddress;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * @author Vince Yuan
 * @date 2021/11/20
 */
public class NioPacketSimplexTest {

    private PacketSimplexServer server;
    private PacketSimplexClient client;

    private Object[] dataArrayToWrite = new Object[] { 12345, 54321L, "Hello NIO" };
    private Random random = new Random();

    @Before
    public void testStartServer() {
        server = new NioPacketSimplexServer(8686);
        server.startServer();
        System.out.println("testStartServer is completed");
    }

    @Test
    public void test1() throws Exception {
        client = new NioPacketSimplexClient(new InetSocketAddress(8686));
        client.startClient();

        long startTimeMillis = System.currentTimeMillis();
        client.write(dataArrayToWrite[random.nextInt(dataArrayToWrite.length)]);
        System.out.println("Time elapsed for one-time communication: " + (System.currentTimeMillis() - startTimeMillis) + "ms");

        client.closeClient();
        System.out.println("testClientConnection is completed");
    }

    @Test
    public void test2() throws Exception {
        List<PacketSimplexClient> clientList = new ArrayList<>();

        long startTimeMillis = System.currentTimeMillis();
        for (int i = 0; i < 100; i++) {
            client = new NioPacketSimplexClient(new InetSocketAddress(8686));
            client.startClient();
            client.write(dataArrayToWrite[random.nextInt(dataArrayToWrite.length)]);
            clientList.add(client);
        }
        // ~60ms for 100 communications
        System.out.println("Time elapsed for highly-intensive communication: " + (System.currentTimeMillis() - startTimeMillis) + "ms");

        for (BaseSimplexClient client : clientList) {
            client.closeClient();
        }
        System.out.println("testIntensiveCommunication is completed");
    }

    @After
    public void testCloseServer() throws Exception {
        Thread.sleep(1000);
        server.closeServer();
        System.out.println("testCloseServer is completed");
    }
}
