package com.demo.haima.test.unit.channel.socket;

import com.demo.haima.test.unit.channel.socket.client.Client;
import com.demo.haima.test.unit.channel.socket.server.Server;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.util.ArrayList;
import java.util.List;

/**
 * @author Vince Yuan
 * @date 2021/12/5
 */
public class SocketChannelTest {

    private Server server;
    private Client[] clients;

    @Before
    public void testStartServerAndClients() throws Exception {
        server = new Server();
        server.start();
        System.out.println("server is started");
        clients = new Client[10];
        for (int i = 0; i < clients.length; i++) {
            clients[i] = new Client("Client" + (i + 1));
            clients[i].start();
            while (!clients[i].isConnected()) { }
        }
        System.out.println("clients are started");
        System.out.println("testStartServerAndClients is completed");
    }

    @Test
    public void testMultipleConnections() throws Exception {
        List<Object> resultList = new ArrayList<>();
        long startTimeMillis = System.currentTimeMillis();
        for (int i = 0; i < 10000; i++) {
            String result = clients[0].write("Hello Channel " + (i + 1));
            resultList.add(result);
        }
        System.out.println("time cost for multiple connections: " + (System.currentTimeMillis() - startTimeMillis) + "ms");
        System.out.println("resultList: " + resultList.size() + " | " + resultList);
    }

    @Test
    public void testMultipleClients() throws Exception {
        List<Object> resultList = new ArrayList<>();
        List<Long> timeCostList = new ArrayList<>();
        for (int i = 0; i < clients.length; i++) {
            long startTimeMillis = System.currentTimeMillis();
            String result = clients[i].write("Hello Channel " + (i + 1));
            long timeCostMillis = System.currentTimeMillis() - startTimeMillis;
            System.out.println("time cost for client" + (i + 1) + ": " + timeCostMillis + "ms");
            timeCostList.add(timeCostMillis);
            resultList.add(result);
        }
        System.out.println("time cost for multiple clients: " + timeCostList.stream().reduce((a, b) -> a + b).get() + "ms | resultList: " + resultList.size() + " | " + resultList);
    }

    @After
    public void testCloseClientsAndServer() throws Exception {
        for (int i = 0; i < clients.length; i++) {
            clients[i].close();
        }
        System.out.println("clients are closed");
        server.close();
        System.out.println("server is closed");
        System.out.println("testCloseClientsAndServer is completed");
    }
}
