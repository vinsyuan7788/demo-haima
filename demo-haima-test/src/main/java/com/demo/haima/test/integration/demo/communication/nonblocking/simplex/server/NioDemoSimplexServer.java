package com.demo.haima.test.integration.demo.communication.nonblocking.simplex.server;

import com.demo.haima.common.utility.LogUtils;
import com.demo.haima.test.integration.demo.communication.nonblocking.simplex.client.NioDemoSimplexClient;
import com.demo.haima.test.integration.demo.communication.nonblocking.simplex.server.socket.DemoSimplexServerSocket;
import com.demo.haima.test.integration.demo.communication.nonblocking.simplex.server.socket.NioDemoSimplexServerSocket;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This class is used to serve as server that can receive data from client. <br/>
 * Here delegates this server to corresponding server socket to keep track of
 * the server state, which is useful if server-level information needs to manipulated.
 * Notice that {@link NioDemoSimplexClient} uses only one thread and one client channel each
 * thread for communication, there is no need to close accepted socket (channel) each
 * time after completing processing received data normally (but should close the accepted
 * socket (channel) in time if there is any critical exception encountered when processing
 * the received data). The manipulation (e.g., closing) of accepted (or client) socket (channel)
 * ls left to be done by {@link NioDemoSimplexClient}.
 *
 * @author Vince Yuan
 * @date 2021/11/21
 */
public class NioDemoSimplexServer extends DemoSimplexServer {

    private static final Logger LOG = LoggerFactory.getLogger(NioDemoSimplexServer.class);

    private DemoSimplexServerSocket serverSocket;

    public NioDemoSimplexServer(int portToBind) {
        this.serverSocket = new NioDemoSimplexServerSocket(this, portToBind);
    }

    @Override
    public void startServer() {
        serverSocket.startSocket();
        LOG.info(LogUtils.getMessage("Server is started"));
    }

    @Override
    public void closeServer() {
        serverSocket.closeSocket();
        LOG.info(LogUtils.getMessage("Server is closed"));
    }
}
