package com.demo.haima.fundamental.server.duplex.asynchronous.socket.handler;

import com.demo.haima.fundamental.server.duplex.asynchronous.socket.AioDemoDuplexServerSocket;
import com.demo.haima.fundamental.server.duplex.asynchronous.socket.handler.AioDemoDuplexServerSocketAcceptCompletionHandler.Attachment;
import com.demo.haima.fundamental.utility.handler.CompletionHandlerHelper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.ByteBuffer;
import java.nio.channels.AsynchronousCloseException;
import java.nio.channels.AsynchronousSocketChannel;
import java.nio.channels.CompletionHandler;
import java.util.concurrent.TimeUnit;

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

            // Scatter-read the data sent from client through the channel into byte buffers
            ByteBuffer byteBufferOfHeader = ByteBuffer.allocateDirect(42);
            ByteBuffer byteBufferOfBody = ByteBuffer.allocateDirect(1024);
            ByteBuffer[] byteBuffers = new ByteBuffer[] { byteBufferOfHeader, byteBufferOfBody };
            acceptedSocketChannel.read(byteBuffers, 0, byteBuffers.length, 30, TimeUnit.SECONDS, AioDemoDuplexServerSocketReadCompletionHandler.Attachment.create(serverSocket, acceptedSocketChannel, byteBufferOfHeader, byteBufferOfBody), readCompletionHandler);
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
