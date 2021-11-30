package com.demo.haima.test.integration.fundamental.simplex.asynchronous;

import com.demo.haima.fundamental.client.simplex.asynchronous.AioBaseSimplexClient;
import com.demo.haima.fundamental.client.simplex.asynchronous.BaseSimplexClient;
import com.demo.haima.fundamental.server.simplex.asynchronous.AioBaseSimplexServer;
import com.demo.haima.fundamental.server.simplex.asynchronous.BaseSimplexServer;
import com.demo.haima.fundamental.utility.auxiliary.AioImplementationWay;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.net.InetSocketAddress;
import java.util.ArrayList;
import java.util.List;

/**
 * @author Vince Yuan
 * @date 2021/11/24
 */
public class AioBaseSimplexTest {

    private BaseSimplexServer server;
    private BaseSimplexClient client;

    @Before
    public void testStartServer() throws Exception {
        server = new AioBaseSimplexServer(AioImplementationWay.COMPLETION_HANDLER, 8686);
        server.startServer();
        System.out.println("testStartServer is completed");
    }

    @Test
    public void test1() throws Exception {
        client = new AioBaseSimplexClient(AioImplementationWay.COMPLETION_HANDLER, new InetSocketAddress("localhost", 8686));
        client.startClient();

        client.closeClient();
        System.out.println("testClientConnection is completed");
    }

    @Test
    public void test2() throws Exception {
        List<BaseSimplexClient> clientList = new ArrayList<>();

        long startTimeMillis = System.currentTimeMillis();
        for (int i = 0; i < 100; i++) {
            client = new AioBaseSimplexClient(AioImplementationWay.COMPLETION_HANDLER, new InetSocketAddress("localhost", 8686));
            client.startClient();
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
