package com.demo.haima.fundamental.server.simplex.asynchronous.socket.handler;

import com.demo.haima.fundamental.server.simplex.asynchronous.socket.AioBaseSimplexServerSocket;
import com.demo.haima.fundamental.server.simplex.asynchronous.socket.handler.AioBaseSimplexServerSocketAcceptCompletionHandler.Attachment;
import com.demo.haima.fundamental.utility.handler.CompletionHandlerHelper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.StandardSocketOptions;
import java.nio.ByteBuffer;
import java.nio.channels.AsynchronousCloseException;
import java.nio.channels.AsynchronousSocketChannel;
import java.nio.channels.CompletionHandler;

/**
 * @author Vince Yuan
 * @date 2021/11/25
 */
public class AioBaseSimplexServerSocketAcceptCompletionHandler extends CompletionHandlerHelper implements CompletionHandler<AsynchronousSocketChannel, Attachment> {

    private static final Logger LOG = LoggerFactory.getLogger(AioBaseSimplexServerSocketAcceptCompletionHandler.class);

    private AioBaseSimplexServerSocketReadCompletionHandler readCompletionHandler;

    public AioBaseSimplexServerSocketAcceptCompletionHandler(AioBaseSimplexServerSocketReadCompletionHandler readCompletionHandler) {
        this.readCompletionHandler = readCompletionHandler;
    }

    @Override
    public void completed(AsynchronousSocketChannel acceptedSocketChannel, Attachment attachment) {
        try {
            // Get content from attachment
            AioBaseSimplexServerSocket serverSocket = attachment.getServerSocket();

            // Update global tracker
            serverSocket.updateTracker();

            // Log message
            logServerSocketChannelInfo(serverSocket.getServerSocketChannel());
            logSocketChannelInfo(acceptedSocketChannel);

            // Configure the accepted socket
            acceptedSocketChannel.setOption(StandardSocketOptions.TCP_NODELAY, true);
            // set socket linger to false, so that socket close does not block
            // -- this option is not supported here in AIO
//            acceptedSocketChannel.setOption(StandardSocketOptions.SO_LINGER, -1);

            // Read the data sent from client through the channel into a byte buffer
            ByteBuffer byteBuffer = ByteBuffer.allocateDirect(1024);
            acceptedSocketChannel.read(byteBuffer, AioBaseSimplexServerSocketReadCompletionHandler.Attachment.create(serverSocket, acceptedSocketChannel, byteBuffer), readCompletionHandler);
        } catch (Throwable t) {
            handleRunningThrowable(t);
        }
    }

    @Override
    public void failed(Throwable t, Attachment attachment) {
        if (t instanceof AsynchronousCloseException) {
            // This exception can be ignored if it is raised by 'closeSocket()' in 'AioBaseSimplexServerSocket'
            // since it will terminate the accept event that is waiting to be completed (i.e., waiting for a connection
            // from client). It does no effect if we close the server socket when we need to close the server
        } else {
            LOG.error("Process event error", t);
        }
    }

    public static class Attachment {

        private AioBaseSimplexServerSocket serverSocket;

        private Attachment(AioBaseSimplexServerSocket serverSocket) {
            this.serverSocket = serverSocket;
        }

        public static Attachment create(AioBaseSimplexServerSocket serverSocket) {
            return new Attachment(serverSocket);
        }

        public AioBaseSimplexServerSocket getServerSocket() {
            return serverSocket;
        }
    }
}
