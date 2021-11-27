package com.demo.haima.fundamental.client.duplex.asynchronous.socket.handler;

import com.demo.haima.fundamental.client.duplex.asynchronous.socket.AioDemoDuplexClientSocket;
import com.demo.haima.fundamental.client.duplex.asynchronous.socket.handler.AioDemoDuplexClientSocketWriteCompletionHandler.Attachment;
import com.demo.haima.fundamental.utils.auxiliary.CompletionHandlerHelper;
import com.demo.haima.fundamental.utils.data.network.packet.Packet;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.ByteBuffer;
import java.nio.channels.CompletionHandler;
import java.util.concurrent.TimeUnit;

/**
 * @author Vince Yuan
 * @date 2021/11/24
 */
public class AioDemoDuplexClientSocketWriteCompletionHandler extends CompletionHandlerHelper implements CompletionHandler<Long, Attachment> {

    private static final Logger LOG = LoggerFactory.getLogger(AioDemoDuplexClientSocketWriteCompletionHandler.class);

    private AioDemoDuplexClientSocketReadCompletionHandler readCompletionHandler;

    @Override
    public void completed(Long numberOfBytesWritten, Attachment attachment) {
        try {
            // Get content from attachment
            AioDemoDuplexClientSocket clientSocket = attachment.getClientSocket();
            Packet packetToSend = attachment.getPacketToSend();

            // Check write event completion result
            if (numberOfBytesWritten < 0) {
                return;
            }
            LOG.info("[Data] | Client writes bytes to server {} | bytes: {}", clientSocket.getServerAddressToConnect(), numberOfBytesWritten);
            LOG.info("[Data] | Client writes packet to server {} | packet: {}", clientSocket.getServerAddressToConnect(), packetToSend);

            // Scatter-read the data sent from server through the channel into byte buffers
            ByteBuffer byteBufferOfHeader = ByteBuffer.allocateDirect(42);
            ByteBuffer byteBufferOfBody = ByteBuffer.allocateDirect(1024);
            ByteBuffer[] byteBuffers = new ByteBuffer[] { byteBufferOfHeader, byteBufferOfBody };
            clientSocket.getClientSocketChannel().read(byteBuffers, 0, byteBuffers.length, 30, TimeUnit.SECONDS, AioDemoDuplexClientSocketReadCompletionHandler.Attachment.create(clientSocket, byteBufferOfHeader, byteBufferOfBody), readCompletionHandler);
        } catch (Throwable t) {
            handleRunningThrowable(t);
        }
    }

    @Override
    public void failed(Throwable t, Attachment attachment) {
        LOG.error("Write event error", t);
    }

    public void setReadCompletionHandler(AioDemoDuplexClientSocketReadCompletionHandler readCompletionHandler) {
        this.readCompletionHandler = readCompletionHandler;
    }

    public static class Attachment {

        private AioDemoDuplexClientSocket clientSocket;
        private Packet packetToSend;

        private Attachment(AioDemoDuplexClientSocket clientSocket, Packet packetToSend) {
            this.clientSocket = clientSocket;
            this.packetToSend = packetToSend;
        }

        public static Attachment create(AioDemoDuplexClientSocket clientSocket, Packet packetToSend) {
            return new Attachment(clientSocket, packetToSend);
        }

        public AioDemoDuplexClientSocket getClientSocket() {
            return clientSocket;
        }

        public Packet getPacketToSend() {
            return packetToSend;
        }
    }
}
