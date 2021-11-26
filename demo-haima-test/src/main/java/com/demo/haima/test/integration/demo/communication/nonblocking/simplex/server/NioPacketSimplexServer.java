package com.demo.haima.test.integration.demo.communication.nonblocking.simplex.server;

import com.demo.haima.common.utility.LogUtils;
import com.demo.haima.test.integration.demo.communication.nonblocking.simplex.client.NioPacketSimplexClient;
import com.demo.haima.test.integration.demo.communication.nonblocking.simplex.server.socket.NioPacketSimplexServerSocket;
import com.demo.haima.test.integration.demo.communication.nonblocking.simplex.server.socket.PacketSimplexServerSocket;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This class is used to serve as server that can receive data from client. <br/>
 * For more details, see {@link NioPacketSimplexClient}
 *
 * @author Vince Yuan
 * @date 2021/11/10
 */
public class NioPacketSimplexServer extends PacketSimplexServer {

    private static final Logger LOG = LoggerFactory.getLogger(NioPacketSimplexServer.class);

    private PacketSimplexServerSocket serverSocket;

    public NioPacketSimplexServer(int portToBind) {
        this.serverSocket = new NioPacketSimplexServerSocket(portToBind);
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
