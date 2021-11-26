package com.demo.haima.test.integration.demo.communication.asynchronous.simplex.client.socket.handler;

import com.demo.haima.test.integration.demo.communication.asynchronous.simplex.client.socket.AioBaseSimplexClientSocket;
import com.demo.haima.test.integration.demo.communication.asynchronous.simplex.client.socket.handler.AioBaseSimpleClientSocketWriteCompletionHandler.Attachment;
import com.demo.haima.test.integration.demo.utils.auxiliary.CompletionHandlerHelper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.ByteBuffer;
import java.nio.channels.CompletionHandler;

/**
 * @author Vince Yuan
 * @date 2021/11/25
 */
public class AioBaseSimpleClientSocketWriteCompletionHandler extends CompletionHandlerHelper implements CompletionHandler<Integer, Attachment> {

    private static final Logger LOG = LoggerFactory.getLogger(AioBaseSimpleClientSocketWriteCompletionHandler.class);

    @Override
    public void completed(Integer numberOfBytesWritten, Attachment attachment) {
        try {
            // Get content from attachment
            AioBaseSimplexClientSocket clientSocket = attachment.getClientSocket();
            String data = attachment.getData();
            ByteBuffer byteBuffer = attachment.getWrittenByteBuffer();

            // Log message
            LOG.info("[Data] | Client writes data to server {} | data: {} | number of bytes written: {} | written byte buffer: {}", clientSocket.getServerAddressToConnect(), data, numberOfBytesWritten, byteBuffer);

            // Update global tracker
            clientSocket.getProcessingTracker().decrementAndGet();
        } catch (Throwable t) {
            handleRunningThrowable(t);
        }
    }

    @Override
    public void failed(Throwable t, Attachment attachment) {
        LOG.error("Process event error", t);
    }

    public static class Attachment {

        private AioBaseSimplexClientSocket clientSocket;
        private String data;
        private ByteBuffer writtenByteBuffer;

        private Attachment(AioBaseSimplexClientSocket clientSocket, String data, ByteBuffer byteBuffer) {
            this.clientSocket = clientSocket;
            this.data = data;
            this.writtenByteBuffer = byteBuffer;
        }

        public static Attachment create(AioBaseSimplexClientSocket clientSocket, String data, ByteBuffer byteBuffer) {
            return new Attachment(clientSocket, data, byteBuffer);
        }

        public AioBaseSimplexClientSocket getClientSocket() {
            return clientSocket;
        }

        public String getData() {
            return data;
        }

        public ByteBuffer getWrittenByteBuffer() {
            return writtenByteBuffer;
        }
    }
}
