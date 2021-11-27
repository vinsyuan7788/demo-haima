package com.demo.haima.fundamental.server.simplex.nonblocking;

import com.demo.haima.common.utility.LogUtils;
import com.demo.haima.fundamental.client.simplex.nonblocking.NioSerdesSimplexClient;
import com.demo.haima.fundamental.server.simplex.nonblocking.socket.NioSerdesSimplexServerSocket;
import com.demo.haima.fundamental.server.simplex.nonblocking.socket.SerdesSimplexServerSocket;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This class is used to serve as server that can receive data from client. <br/>
 * It is basically the same as {@link NioPacketSimplexServer}, while it is used to
 * received data from {@link NioSerdesSimplexClient}.
 * Notice that here delegates this server to corresponding server socket to keep track of
 * the server state, which is useful if server-level information needs to manipulated.
 * Since {@link NioSerdesSimplexClient} uses a socket each time there is data that needs to be
 * sent, server here needs (or say is better) to close the accepted socket (channel) in time
 * after completing processing the received data normally (and should close the accepted
 * socket (channel) in time if there is any critical exception encountered when processing
 * the received data)
 *
 * @author Vince Yuan
 * @date 2021/11/21
 */
public class NioSerdesSimplexServer extends SerdesSimplexServer {

    private static final Logger LOG = LoggerFactory.getLogger(NioSerdesSimplexServer.class);

    private SerdesSimplexServerSocket serverSocket;

    public NioSerdesSimplexServer(int portToBind) {
        this.serverSocket = new NioSerdesSimplexServerSocket(this, portToBind);
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
