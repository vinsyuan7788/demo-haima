package com.demo.haima.fundamental.client.simplex.asynchronous.socket.handler;

import com.demo.haima.fundamental.client.simplex.asynchronous.socket.AioBaseSimplexClientSocket;
import com.demo.haima.fundamental.client.simplex.asynchronous.socket.handler.AioBaseSimplexClientSocketConnectCompletionHandler.Attachment;
import com.demo.haima.fundamental.utils.auxiliary.CompletionHandlerHelper;
import com.demo.haima.fundamental.utils.state.client.asynchronous.ClientSocketState;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.ByteBuffer;
import java.nio.channels.CompletionHandler;

/**
 * @author Vince Yuan
 * @date 2021/11/25
 */
public class AioBaseSimplexClientSocketConnectCompletionHandler extends CompletionHandlerHelper implements CompletionHandler<Void, Attachment> {

    private static final Logger LOG = LoggerFactory.getLogger(AioBaseSimplexClientSocketConnectCompletionHandler.class);

    private AioBaseSimpleClientSocketWriteCompletionHandler writeCompletionHandler;

    public AioBaseSimplexClientSocketConnectCompletionHandler(AioBaseSimpleClientSocketWriteCompletionHandler writeCompletionHandler) {
        this.writeCompletionHandler = writeCompletionHandler;
    }

    @Override
    public void completed(Void connectResult, Attachment attachment) {
        try {
            // Get content from attachment
            AioBaseSimplexClientSocket clientSocket = attachment.getClientSocket();

            // Update global tracker
            clientSocket.getConnectingTracker().decrementAndGet();

            // Set client socket state
            LOG.info("Client socket is connected to {}", clientSocket.getServerAddressToConnect());
            clientSocket.setClientSocketState(ClientSocketState.CONNECTED);
            logSocketChannelInfo(clientSocket.getClientSocketChannel());

            // Wrap the data into a byte buffer
            // NOTICE: the wrap method will set buffer's position to zero, hence no need to flip it
            String data = "Hello NIO";
            ByteBuffer byteBuffer = ByteBuffer.wrap(data.getBytes());
            // Send the byte buffer to server
            clientSocket.getClientSocketChannel().write(byteBuffer, AioBaseSimpleClientSocketWriteCompletionHandler.Attachment.create(clientSocket, data, byteBuffer), writeCompletionHandler);
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

        private Attachment(AioBaseSimplexClientSocket clientSocket) {
            this.clientSocket = clientSocket;
        }

        public static Attachment create(AioBaseSimplexClientSocket clientSocket) {
            return new Attachment(clientSocket);
        }

        public AioBaseSimplexClientSocket getClientSocket() {
            return clientSocket;
        }
    }
}
