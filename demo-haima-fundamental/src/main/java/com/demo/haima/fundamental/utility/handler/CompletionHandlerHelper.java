package com.demo.haima.fundamental.utility.handler;

import com.demo.haima.common.utility.LogUtils;
import com.demo.haima.fundamental.utility.exception.handler.CompletionExceptionHandler;
import com.demo.haima.fundamental.utility.information.AsynchronousChannelInfoProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.EOFException;
import java.net.SocketAddress;
import java.nio.channels.AsynchronousServerSocketChannel;
import java.nio.channels.AsynchronousSocketChannel;

/**
 * @author Vince Yuan
 * @date 2021/11/25
 */
public abstract class CompletionHandlerHelper implements CompletionExceptionHandler, AsynchronousChannelInfoProvider {

    private static final Logger LOG = LoggerFactory.getLogger(CompletionHandlerHelper.class);

    private String className = getClass().getSimpleName();

    @Override
    public void handleRunningThrowable(Throwable t) {
        if (t instanceof EOFException) {
            LOG.error("Reason unknown yet, but this exception may occasionally occur in duplex communication when client " +
                    "tries to read the byte buffer that has been read by client socket channel to a packet, while in " +
                    "NIO-implementation this exception never occurs. When it occurs, just re-run the test for now", t);
        } else {
            LOG.error(className + " | Process event error", t);
        }
    }

    @Override
    public void logSocketChannelInfo(AsynchronousSocketChannel socketChannel) throws Exception {
        SocketAddress localSocketAddress = socketChannel.getLocalAddress();
        SocketAddress remoteSocketAddress = socketChannel.getRemoteAddress();
        LOG.info(LogUtils.getMessage(className + "#logSocketChannelInfo", "local address of socket channel: {}"
                + " | remote address of socket channel: {}"), localSocketAddress, remoteSocketAddress);
    }

    @Override
    public void logServerSocketChannelInfo(AsynchronousServerSocketChannel serverSocketChannel) throws Exception {
        SocketAddress localSocketAddress = serverSocketChannel.getLocalAddress();
        LOG.info(LogUtils.getMessage(className + "#logServerSocketChannelInfo", "local address of server socket channel: {}"), localSocketAddress);
    }
}
