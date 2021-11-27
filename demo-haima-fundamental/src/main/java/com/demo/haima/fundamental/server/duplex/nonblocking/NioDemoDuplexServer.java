package com.demo.haima.fundamental.server.duplex.nonblocking;

import com.demo.haima.common.utility.LogUtils;
import com.demo.haima.fundamental.client.simplex.nonblocking.NioDemoSimplexClient;
import com.demo.haima.fundamental.server.duplex.nonblocking.socket.DemoDuplexServerSocket;
import com.demo.haima.fundamental.server.duplex.nonblocking.socket.NioDemoDuplexServerSocket;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This class is used to serve as server that can receive data from client. <br/>
 * Here delegates this server to corresponding server socket to keep track of
 * the server state, which is useful if server-level information needs to manipulated.
 * See {@link NioDemoSimplexClient} for more details.
 *
 * @author Vince Yuan
 * @date 2021/11/21
 */
public class NioDemoDuplexServer extends DemoDuplexServer {

    private static final Logger LOG = LoggerFactory.getLogger(NioDemoDuplexServer.class);

    private DemoDuplexServerSocket serverSocket;

    public NioDemoDuplexServer(int portToBind) {
        this.serverSocket = new NioDemoDuplexServerSocket(this, portToBind);
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
