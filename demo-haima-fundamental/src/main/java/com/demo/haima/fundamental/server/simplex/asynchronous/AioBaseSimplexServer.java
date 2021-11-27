package com.demo.haima.fundamental.server.simplex.asynchronous;

import com.demo.haima.common.utility.LogUtils;
import com.demo.haima.fundamental.server.simplex.asynchronous.socket.AioBaseSimplexServerSocket;
import com.demo.haima.fundamental.server.simplex.asynchronous.socket.BaseSimplexServerSocket;
import com.demo.haima.fundamental.utils.auxiliary.AioImplementationWay;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This class is used to serve as server that can receive data from client. <br/>
 * This class is used to expose the fundamental mechanism of NIO, hence (or one can
 * assume that) this is the simplest implementation.
 *
 * @author Vince Yuan
 * @date 2021/11/10
 */
public class AioBaseSimplexServer extends BaseSimplexServer {

    private static final Logger LOG = LoggerFactory.getLogger(AioBaseSimplexServer.class);

    private BaseSimplexServerSocket serverSocket;

    public AioBaseSimplexServer(AioImplementationWay runWhichImplementation, int portToBind) {
        this.serverSocket = new AioBaseSimplexServerSocket(runWhichImplementation, portToBind);
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
