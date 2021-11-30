package com.demo.haima.fundamental.server.simplex.asynchronous.socket.handler;

import com.demo.haima.fundamental.server.simplex.asynchronous.socket.AioBaseSimplexServerSocket;
import com.demo.haima.fundamental.server.simplex.asynchronous.socket.handler.AioBaseSimplexServerSocketReadCompletionHandler.Attachment;
import com.demo.haima.fundamental.utility.auxiliary.CompletionHandlerHelper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.ByteBuffer;
import java.nio.channels.AsynchronousSocketChannel;
import java.nio.channels.CompletionHandler;

/**
 * @author Vince Yuan
 * @date 2021/11/25
 */
public class AioBaseSimplexServerSocketReadCompletionHandler extends CompletionHandlerHelper implements CompletionHandler<Integer, Attachment> {

    private static final Logger LOG = LoggerFactory.getLogger(AioBaseSimplexServerSocketReadCompletionHandler.class);

    @Override
    public void completed(Integer numberOfBytesRead, Attachment attachment) {
        try {
            // Get content from attachment
            AioBaseSimplexServerSocket serverSocket = attachment.getServerSocket();
            AsynchronousSocketChannel acceptedSocketChannel = attachment.getAcceptedSocketChannel();
            ByteBuffer byteBuffer = attachment.getReadByteBuffer();

            // Log message
            LOG.info("[Data] | Server reads bytes from client {} | bytes: {} | ", acceptedSocketChannel.getRemoteAddress(), numberOfBytesRead);

            // Update global tracker
            serverSocket.getProcessingTracker().decrementAndGet();

            // Flip the byte buffer so that the data within it can be read to target byte array
            byteBuffer.flip();
            // Read the data in the buffer to target byte array
            byte[] dataBytesRead = new byte[numberOfBytesRead];
            byteBuffer.get(dataBytesRead);
            String data = new String(dataBytesRead);
            LOG.info("[Data] | Server reads data from client {} | data: {}", acceptedSocketChannel.getRemoteAddress(), data);

            // Close the accepted socket channel
            serverSocket.closeAcceptedSocketChannel(acceptedSocketChannel);
        } catch (Throwable t) {
            handleRunningThrowable(t);
        }
    }

    @Override
    public void failed(Throwable t, Attachment attachment) {
        LOG.error("Process event error", t);
    }

    public static class Attachment {

        private AioBaseSimplexServerSocket serverSocket;
        private AsynchronousSocketChannel acceptedSocketChannel;
        private ByteBuffer readByteBuffer;

        private Attachment(AioBaseSimplexServerSocket serverSocket, AsynchronousSocketChannel acceptedSocketChannel, ByteBuffer byteBuffer) {
            this.serverSocket = serverSocket;
            this.acceptedSocketChannel = acceptedSocketChannel;
            this.readByteBuffer = byteBuffer;
        }

        public static Attachment create(AioBaseSimplexServerSocket serverSocket, AsynchronousSocketChannel acceptedSocketChannel, ByteBuffer byteBuffer) {
            return new Attachment(serverSocket, acceptedSocketChannel, byteBuffer);
        }

        public AioBaseSimplexServerSocket getServerSocket() {
            return serverSocket;
        }

        public AsynchronousSocketChannel getAcceptedSocketChannel() {
            return acceptedSocketChannel;
        }

        public ByteBuffer getReadByteBuffer() {
            return readByteBuffer;
        }
    }
}
