package com.demo.haima.test.integration.demo.communication.asynchronous.duplex.server.socket.handler;

import com.demo.haima.test.integration.demo.communication.asynchronous.duplex.server.socket.AioDemoDuplexServerSocket;
import com.demo.haima.test.integration.demo.communication.asynchronous.duplex.server.socket.handler.AioDemoDuplexServerSocketAcceptCompletionHandler.Attachment;
import com.demo.haima.test.integration.demo.utils.auxiliary.CompletionHandlerHelper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.ByteBuffer;
import java.nio.channels.AsynchronousCloseException;
import java.nio.channels.AsynchronousSocketChannel;
import java.nio.channels.CompletionHandler;

/**
 * @author Vince Yuan
 * @date 2021/11/24
 */
public class AioDemoDuplexServerSocketAcceptCompletionHandler extends CompletionHandlerHelper implements CompletionHandler<AsynchronousSocketChannel, Attachment> {

    private static final Logger LOG = LoggerFactory.getLogger(AioDemoDuplexServerSocketAcceptCompletionHandler.class);

    private AioDemoDuplexServerSocketReadCompletionHandler readCompletionHandler;

    public AioDemoDuplexServerSocketAcceptCompletionHandler(AioDemoDuplexServerSocketReadCompletionHandler readCompletionHandler) {
        this.readCompletionHandler = readCompletionHandler;
    }

    @Override
    public void completed(AsynchronousSocketChannel acceptedSocketChannel, Attachment attachment) {
        try {
            // Get content from attachment
            AioDemoDuplexServerSocket serverSocket = attachment.getServerSocket();

            // Update global tracker
            serverSocket.getAcceptingTracker().decrementAndGet();

            // Read the data sent from client through the channel into a byte buffer
            ByteBuffer byteBuffer = ByteBuffer.allocateDirect(1024);
            acceptedSocketChannel.read(byteBuffer, AioDemoDuplexServerSocketReadCompletionHandler.Attachment.create(serverSocket, acceptedSocketChannel, byteBuffer), readCompletionHandler);
        } catch (Throwable t) {
            handleRunningThrowable(t);
        }
    }

    @Override
    public void failed(Throwable t, Attachment attachment) {
        if (t instanceof AsynchronousCloseException) {
            // Do nothing here
        } else {
            LOG.error("Process event error", t);
        }
    }

    public static class Attachment {

        private AioDemoDuplexServerSocket serverSocket;

        private Attachment(AioDemoDuplexServerSocket serverSocket) {
            this.serverSocket = serverSocket;
        }

        public static Attachment create(AioDemoDuplexServerSocket serverSocket) {
            return new Attachment(serverSocket);
        }

        public AioDemoDuplexServerSocket getServerSocket() {
            return serverSocket;
        }
    }
}
