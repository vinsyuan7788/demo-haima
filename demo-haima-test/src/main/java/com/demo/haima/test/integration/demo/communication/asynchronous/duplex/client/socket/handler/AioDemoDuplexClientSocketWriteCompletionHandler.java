package com.demo.haima.test.integration.demo.communication.asynchronous.duplex.client.socket.handler;

import com.demo.haima.test.integration.demo.communication.asynchronous.duplex.client.socket.AioDemoDuplexClientSocket;
import com.demo.haima.test.integration.demo.communication.asynchronous.duplex.client.socket.handler.AioDemoDuplexClientSocketWriteCompletionHandler.Attachment;
import com.demo.haima.test.integration.demo.utils.auxiliary.CompletionHandlerHelper;
import com.demo.haima.test.integration.demo.utils.data.network.packet.Packet;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.ByteBuffer;
import java.nio.channels.CompletionHandler;

/**
 * @author Vince Yuan
 * @date 2021/11/24
 */
public class AioDemoDuplexClientSocketWriteCompletionHandler extends CompletionHandlerHelper implements CompletionHandler<Integer, Attachment> {

    private static final Logger LOG = LoggerFactory.getLogger(AioDemoDuplexClientSocketWriteCompletionHandler.class);

    private AioDemoDuplexClientSocketReadCompletionHandler readCompletionHandler;

    @Override
    public void completed(Integer numberOfBytesWritten, Attachment attachment) {
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

            // Read the data sent from client through the channel into a byte buffer
            ByteBuffer byteBuffer = ByteBuffer.allocateDirect(1024);
            clientSocket.getClientSocketChannel().read(byteBuffer, AioDemoDuplexClientSocketReadCompletionHandler.Attachment.create(clientSocket, byteBuffer), readCompletionHandler);
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
