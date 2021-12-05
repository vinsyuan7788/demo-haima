package com.demo.haima.test.unit.channel.socket;

import com.demo.haima.test.unit.channel.socket.client.Client;
import com.demo.haima.test.unit.channel.socket.server.Server;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

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
        clients = new Client[3];
        for (int i = 0; i < clients.length; i++) {
            clients[i] = new Client("Client" + (i + 1));
            clients[i].start();
        }
    }

    @Test
    public void testMultipleClients() throws Exception {
        for (int i = 0; i < clients.length; i++) {
            clients[i].write();
        }
    }

    @After
    public void closeClientsAndServer() throws Exception {
        Thread.sleep(5000);
        for (int i = 0; i < clients.length; i++) {
            clients[i].close();
        }
        server.close();
    }
}
