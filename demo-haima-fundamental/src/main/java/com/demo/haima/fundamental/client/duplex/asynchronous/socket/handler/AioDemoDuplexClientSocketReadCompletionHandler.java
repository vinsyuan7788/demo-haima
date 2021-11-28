package com.demo.haima.fundamental.client.duplex.asynchronous.socket.handler;

import com.demo.haima.fundamental.client.duplex.asynchronous.socket.AioDemoDuplexClientSocket;
import com.demo.haima.fundamental.client.duplex.asynchronous.socket.handler.AioDemoDuplexClientSocketReadCompletionHandler.Attachment;
import com.demo.haima.fundamental.utils.auxiliary.CompletionHandlerHelper;
import com.demo.haima.fundamental.utils.data.network.definition.ByteBufferType;
import com.demo.haima.fundamental.utils.data.network.definition.OperationType;
import com.demo.haima.fundamental.utils.data.network.packet.Packet;
import com.demo.haima.fundamental.utils.data.network.response.body.ResponseBody;
import com.demo.haima.fundamental.utils.data.network.response.body.SnowflakeIdResponseBody;
import com.demo.haima.fundamental.utils.data.network.response.body.TransmitDataResponseBody;
import com.demo.haima.fundamental.utils.data.network.response.header.ResponseHeader;
import com.demo.haima.fundamental.utils.exception.DemoException;
import com.demo.haima.fundamental.utils.state.packet.PacketProcessState;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.ByteBuffer;
import java.nio.channels.AsynchronousSocketChannel;
import java.nio.channels.CompletionHandler;
import java.util.concurrent.TimeUnit;

/**
 * @author Vince Yuan
 * @date 2021/11/24
 */
public class AioDemoDuplexClientSocketReadCompletionHandler extends CompletionHandlerHelper implements CompletionHandler<Long, Attachment> {

    private static final Logger LOG = LoggerFactory.getLogger(AioDemoDuplexClientSocketReadCompletionHandler.class);

    private AioDemoDuplexClientSocketWriteCompletionHandler writeCompletionHandler;

    @Override
    public void completed(Long numberOfBytesRead, Attachment attachment) {
        try {
            // Get content from attachment
            AioDemoDuplexClientSocket clientSocket = attachment.getClientSocket();
            ByteBuffer byteBufferOfHeader = attachment.getReadByteBufferOfHeader();
            ByteBuffer byteBufferOfBody = attachment.getReadByteBufferOfBody();

            // Check read event completion result
            if (numberOfBytesRead < 0) {
                return;
            }
            LOG.info("[Data] | Client reads bytes from server {} | bytes: {} | byte buffer of header read: {} | byte buffer of body read: {}", clientSocket.getServerAddressToConnect(), numberOfBytesRead, byteBufferOfHeader, byteBufferOfBody);

            // Read the data from the byte buffer
            Packet packet = Packet.readOnClient(byteBufferOfHeader, byteBufferOfBody);
            LOG.info("[Data] | Client reads packet from server {} | packet: {} | byte buffer of header read: {} | byte buffer of body read: {}", clientSocket.getServerAddressToConnect(), packet, byteBufferOfHeader, byteBufferOfBody);

            // Process received packet
            processReceivedPacket(packet, clientSocket);

            // Close the client socket channel
//            clientSocket.closeSocketChannel(clientSocket.getClientSocketChannel());

            // Get the client socket channel
            AsynchronousSocketChannel clientSocketChannel = clientSocket.getClientSocketChannel();
            if (clientSocketChannel == null) {
                return;
            }

            // Get the packet to send
            Packet packetToSend = clientSocket.getOutgoingPacketQueue().poll();
            while (packetToSend == null) {
                packetToSend = clientSocket.getOutgoingPacketQueue().poll();
            }
            packetToSend.setPacketProcessState(PacketProcessState.PROCESSING);

            // Keep track of this packet that is under processing
            clientSocket.getConnectionIdAndProcessingPacketMap().put(packetToSend.getRequestHeader().getConnectionId(), packetToSend);
            LOG.info("[Process] | Packet starts waiting to be processed | packet: {}", packetToSend);

            // Get the byte buffers from packet
            ByteBuffer[] buffersToWrite = packetToSend.createByteBuffersOnClient(ByteBufferType.DIRECT);
            // Gather-write the byte buffers to server
            clientSocketChannel.write(buffersToWrite, 0, buffersToWrite.length, 30, TimeUnit.SECONDS, AioDemoDuplexClientSocketWriteCompletionHandler.Attachment.create(clientSocket, packetToSend), writeCompletionHandler);
        } catch (Throwable t) {
            handleRunningThrowable(t);
        }
    }

    @Override
    public void failed(Throwable t, Attachment attachment) {
        LOG.error("Read event error", t);
    }

    public void setWriteCompletionHandler(AioDemoDuplexClientSocketWriteCompletionHandler writeCompletionHandler) {
        this.writeCompletionHandler = writeCompletionHandler;
    }

    private void processReceivedPacket(Packet receivedPacket, AioDemoDuplexClientSocket clientSocket) {
        // Get the response header
        ResponseHeader responseHeader = receivedPacket.getResponseHeader();
        if (responseHeader == null) {
            throw new DemoException("Response header is null");
        }
        // Get the connection ID from response header
        String connectionId = responseHeader.getConnectionId();
        // Check if received packet is the one that needs to be processed
        Packet processingPacket = clientSocket.getConnectionIdAndProcessingPacketMap().get(connectionId);
        if (processingPacket == null) {
            LOG.error("[Process] | Received packet is not the one that needs to be processed | packet: {}", receivedPacket);
            return;
        }
        // Get the operation type from response header
        int operationType = responseHeader.getOperationType();
        // Perform business processing according to the operation type
        ResponseBody responseBody = receivedPacket.getResponseBody();
        switch (operationType) {
            case OperationType.TRANSMIT_DATA:
                // Get response body
                TransmitDataResponseBody transmitDataResponseBody = (TransmitDataResponseBody) responseBody;
                // Perform business processing here (especially if there is some data that needs to be processed in the response body after server returns packet)
                LOG.info("[Business] | Client completes processing data | transmit result: {}", transmitDataResponseBody.isSuccess());
                // Set the corresponding response body to the processing packet
                processingPacket.setResponseBody(transmitDataResponseBody);
                break;
            case OperationType.SNOWFLAKE_ID:
                // Get response body
                SnowflakeIdResponseBody snowflakeIdResponseBody = (SnowflakeIdResponseBody) responseBody;
                // Perform business processing here (especially if there is some data that needs to be processed in the response body after server returns packet)
                LOG.info("[Business] | Client completes processing data | snowflake ID: {}", snowflakeIdResponseBody.getSnowflakeId());
                // Set the corresponding response body to the processing packet
                processingPacket.setResponseBody(snowflakeIdResponseBody);
                break;
            default:
                throw new DemoException("Operation type is unknown");
        }
        // Set the response header to the processing packet
        processingPacket.setResponseHeader(responseHeader);
        // Mark the packet as processed
        processingPacket.setPacketProcessState(PacketProcessState.PROCESSED);
        // Wake up the thread that is waiting for the packet to be processed
        synchronized (processingPacket) {
            processingPacket.notifyAll();
        }
        // Remove the track of this packet that has been processed
        clientSocket.getConnectionIdAndProcessingPacketMap().remove(connectionId);
        LOG.info("[Process] | Packet is processed successfully | packet: {}", processingPacket);
    }

    public static class Attachment {

        private AioDemoDuplexClientSocket clientSocket;
        private ByteBuffer readByteBufferOfHeader;
        private ByteBuffer readByteBufferOfBody;

        private Attachment(AioDemoDuplexClientSocket clientSocket, ByteBuffer byteBufferOfHeader, ByteBuffer byteBufferOfBody) {
            this.clientSocket = clientSocket;
            this.readByteBufferOfHeader = byteBufferOfHeader;
            this.readByteBufferOfBody = byteBufferOfBody;
        }

        public static Attachment create(AioDemoDuplexClientSocket clientSocket, ByteBuffer byteBufferOfHeader, ByteBuffer byteBufferOfBody) {
            return new Attachment(clientSocket, byteBufferOfHeader, byteBufferOfBody);
        }

        public AioDemoDuplexClientSocket getClientSocket() {
            return clientSocket;
        }

        public ByteBuffer getReadByteBufferOfHeader() {
            return readByteBufferOfHeader;
        }

        public ByteBuffer getReadByteBufferOfBody() {
            return readByteBufferOfBody;
        }
    }
}
