package com.demo.haima.fundamental.server.duplex.asynchronous;

import com.demo.haima.fundamental.server.duplex.asynchronous.socket.AioDemoDuplexServerSocket;
import com.demo.haima.fundamental.server.duplex.asynchronous.socket.DemoDuplexServerSocket;
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
