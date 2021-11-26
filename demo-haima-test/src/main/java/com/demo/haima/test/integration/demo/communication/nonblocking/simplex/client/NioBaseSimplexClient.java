package com.demo.haima.test.integration.demo.communication.nonblocking.simplex.client;

import com.demo.haima.common.utility.LogUtils;
import com.demo.haima.test.integration.demo.communication.nonblocking.simplex.client.socket.BaseSimplexClientSocket;
import com.demo.haima.test.integration.demo.communication.nonblocking.simplex.client.socket.NioBaseSimplexClientSocket;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.SocketAddress;

/**
 * This client is used to serve as client that can send data to server. <br/>
 * Each client corresponds to one socket only, and it will only send
 * data to the server (therefore it is a simplex client).
 * This client is used to present the fundamental mechanism of client.
 *
 * @author Vince Yuan
 * @date 2021/11/10
 */
public class NioBaseSimplexClient extends BaseSimplexClient {

    private static final Logger LOG = LoggerFactory.getLogger(NioBaseSimplexClient.class);

    public BaseSimplexClientSocket clientSocket;

    public NioBaseSimplexClient(SocketAddress serverAddressToConnect) {
        clientSocket = new NioBaseSimplexClientSocket(serverAddressToConnect);
    }

    @Override
    public void startClient() {
        clientSocket.startSocket();
        LOG.info(LogUtils.getMessage("Client is started"));
    }

    @Override
    public void closeClient() {
        clientSocket.closeSocket();
        LOG.info(LogUtils.getMessage("Client is closed"));
    }
}
