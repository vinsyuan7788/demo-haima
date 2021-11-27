package com.demo.haima.fundamental.client.simplex.asynchronous;

import com.demo.haima.common.utility.LogUtils;
import com.demo.haima.fundamental.client.simplex.asynchronous.socket.AioBaseSimplexClientSocket;
import com.demo.haima.fundamental.client.simplex.asynchronous.socket.BaseSimplexClientSocket;
import com.demo.haima.fundamental.utils.auxiliary.AioImplementationWay;
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
public class AioBaseSimplexClient extends BaseSimplexClient {

    private static final Logger LOG = LoggerFactory.getLogger(AioBaseSimplexClient.class);

    public BaseSimplexClientSocket clientSocket;

    public AioBaseSimplexClient(AioImplementationWay runWhichImplementation, SocketAddress serverAddressToConnect) {
        clientSocket = new AioBaseSimplexClientSocket(runWhichImplementation, serverAddressToConnect);
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
