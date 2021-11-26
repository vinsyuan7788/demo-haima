package com.demo.haima.test.integration.demo.communication.asynchronous.duplex.server;

import com.demo.haima.test.integration.demo.communication.asynchronous.duplex.server.socket.AioDemoDuplexServerSocket;
import com.demo.haima.test.integration.demo.communication.asynchronous.duplex.server.socket.DemoDuplexServerSocket;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author Vince Yuan
 * @date 2021/11/24
 */
public class AioDemoDuplexServer extends DemoDuplexServer {

    private static final Logger LOG = LoggerFactory.getLogger(AioDemoDuplexServer.class);

    private DemoDuplexServerSocket serverSocket;

    public AioDemoDuplexServer(int portToBind) {
        serverSocket = new AioDemoDuplexServerSocket(this, portToBind);
    }

    @Override
    public void startServer() {
        serverSocket.startSocket();
    }

    @Override
    public void closeServer() {
        serverSocket.closeSocket();
    }
}
