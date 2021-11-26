package com.demo.haima.test.integration.demo.communication.asynchronous.duplex.client.socket.handler;

import com.demo.haima.test.integration.demo.communication.asynchronous.duplex.client.socket.AioDemoDuplexClientSocket;
import com.demo.haima.test.integration.demo.communication.asynchronous.duplex.client.socket.handler.AioDemoDuplexClientSocketConnectCompletionHandler.Attachment;
import com.demo.haima.test.integration.demo.utils.auxiliary.CompletionHandlerHelper;
import com.demo.haima.test.integration.demo.utils.data.network.packet.Packet;
import com.demo.haima.test.integration.demo.utils.state.client.aio.ClientSocketState;
import com.demo.haima.test.integration.demo.utils.state.packet.PacketProcessState;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.ByteBuffer;
import java.nio.channels.AsynchronousSocketChannel;
import java.nio.channels.CompletionHandler;

/**
 * @author Vince Yuan
 * @date 2021/11/24
 */
public class AioDemoDuplexClientSocketConnectCompletionHandler extends CompletionHandlerHelper implements CompletionHandler<Void, Attachment> {

    private static final Logger LOG = LoggerFactory.getLogger(AioDemoDuplexClientSocketConnectCompletionHandler.class);

    private AioDemoDuplexClientSocketWriteCompletionHandler writeCompletionHandler;

    public AioDemoDuplexClientSocketConnectCompletionHandler(AioDemoDuplexClientSocketWriteCompletionHandler writeCompletionHandler) {
        this.writeCompletionHandler = writeCompletionHandler;
    }

    @Override
    public void completed(Void connectResult, Attachment attachment) {
        try {
            // Get content from attachment
            AioDemoDuplexClientSocket clientSocket = attachment.getClientSocket();

            // Get the client socket channel that finishes connecting to server
            AsynchronousSocketChannel clientSocketChannel = clientSocket.getClientSocketChannel();
            if (clientSocketChannel == null) {
                return;
            }
            LOG.info("Client socket is connected to {}", clientSocket.getServerAddressToConnect());
            clientSocket.setClientSocketState(ClientSocketState.CONNECTED);
            logSocketChannelInfo(clientSocketChannel);

            // Get the packet to send
            Packet packetToSend = clientSocket.getOutgoingPacketQueue().poll();
            while (packetToSend == null) {
                packetToSend = clientSocket.getOutgoingPacketQueue().poll();
            }
            packetToSend.setPacketProcessState(PacketProcessState.PROCESSING);

            // Keep track of this packet that is under processing
            clientSocket.getConnectionIdAndProcessingPacketMap().put(packetToSend.getRequestHeader().getConnectionId(), packetToSend);
            LOG.info("[Process] | Packet starts waiting to be processed | packet: {}", packetToSend);

            // Get the byte buffer from packet
            ByteBuffer byteBuffer = packetToSend.getByteBuffer();
            // Send the byte buffer to server
            clientSocketChannel.write(byteBuffer, AioDemoDuplexClientSocketWriteCompletionHandler.Attachment.create(clientSocket, packetToSend), writeCompletionHandler);
        } catch (Throwable t) {
            handleRunningThrowable(t);
        }
    }

    @Override
    public void failed(Throwable t, Attachment attachment) {
        LOG.error("Connect event error", t);
    }

    public static class Attachment {

        private AioDemoDuplexClientSocket clientSocket;

        private Attachment(AioDemoDuplexClientSocket clientSocket) {
            this.clientSocket = clientSocket;
        }

        public static Attachment create(AioDemoDuplexClientSocket clientSocket) {
            return new Attachment(clientSocket);
        }

        public AioDemoDuplexClientSocket getClientSocket() {
            return clientSocket;
        }
    }
}
