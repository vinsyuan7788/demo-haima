package com.demo.haima.fundamental.server.duplex.asynchronous.socket.handler;

import com.demo.haima.fundamental.server.duplex.asynchronous.socket.AioDemoDuplexServerSocket;
import com.demo.haima.fundamental.server.duplex.asynchronous.socket.handler.AioDemoDuplexServerSocketReadCompletionHandler.Attachment;
import com.demo.haima.fundamental.utils.auxiliary.CompletionHandlerHelper;
import com.demo.haima.fundamental.utils.business.snowflake.SnowFlakeIdWorker;
import com.demo.haima.fundamental.utils.business.snowflake.factory.SnowFlakeIdWorkerFactory;
import com.demo.haima.fundamental.utils.data.network.definition.ByteBufferType;
import com.demo.haima.fundamental.utils.data.network.definition.OperationType;
import com.demo.haima.fundamental.utils.data.network.packet.Packet;
import com.demo.haima.fundamental.utils.data.network.request.body.RequestBody;
import com.demo.haima.fundamental.utils.data.network.request.body.SnowflakeIdRequestBody;
import com.demo.haima.fundamental.utils.data.network.request.body.TransmitDataRequestBody;
import com.demo.haima.fundamental.utils.data.network.request.header.RequestHeader;
import com.demo.haima.fundamental.utils.data.network.response.body.ResponseBody;
import com.demo.haima.fundamental.utils.data.network.response.body.SnowflakeIdResponseBody;
import com.demo.haima.fundamental.utils.data.network.response.body.TransmitDataResponseBody;
import com.demo.haima.fundamental.utils.data.network.response.header.ResponseHeader;
import com.demo.haima.fundamental.utils.exception.DemoException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.ByteBuffer;
import java.nio.channels.AsynchronousSocketChannel;
import java.nio.channels.CompletionHandler;
import java.util.concurrent.TimeUnit;

/**
 * @author Vince Yuan
 * @date 2021/11/25
 */
public class AioDemoDuplexServerSocketReadCompletionHandler extends CompletionHandlerHelper implements CompletionHandler<Long, Attachment> {

    private static final Logger LOG = LoggerFactory.getLogger(AioDemoDuplexServerSocketReadCompletionHandler.class);

    private AioDemoDuplexServerSocketWriteCompletionHandler writeCompletionHandler;

    @Override
    public void completed(Long numberOfBytesRead, Attachment attachment) {
        try {
            // Get content from attachment
            AioDemoDuplexServerSocket serverSocket = attachment.getServerSocket();
            AsynchronousSocketChannel acceptedSocketChannel = attachment.getAcceptedSocketChannel();
            ByteBuffer byteBufferOfHeader = attachment.getReadByteBufferOfHeader();
            ByteBuffer byteBufferOfBody = attachment.getReadByteBufferOfBody();

            // Check read event completion result
            if (numberOfBytesRead < 0) {
                return;
            }
            LOG.info("[Data] | Server reads bytes from client {} | bytes: {}", acceptedSocketChannel.getRemoteAddress(), numberOfBytesRead);

            // Read the data from the byte buffer
            Packet packet = Packet.readOnServer(byteBufferOfHeader, byteBufferOfBody);
            LOG.info("[Data] | Server reads packet from client {} | packet: {}", acceptedSocketChannel.getRemoteAddress(), packet);

            // Process received packet and return a new one
            Packet packetToSend = processReceivedPacket(packet);

            // Get the byte buffers from packet
            ByteBuffer[] buffersToWrite = packetToSend.getByteBuffersOnServer(ByteBufferType.DIRECT);
            // Gather-write the byte buffers to client
            acceptedSocketChannel.write(buffersToWrite, 0, buffersToWrite.length, 30, TimeUnit.SECONDS, AioDemoDuplexServerSocketWriteCompletionHandler.Attachment.create(serverSocket, acceptedSocketChannel, packetToSend), writeCompletionHandler);
        } catch (Throwable t) {
            handleRunningThrowable(t);
        }
    }

    @Override
    public void failed(Throwable t, Attachment attachment) {
        LOG.error("Process event error", t);
    }

    public void setWriteCompletionHandler(AioDemoDuplexServerSocketWriteCompletionHandler writeCompletionHandler) {
        this.writeCompletionHandler = writeCompletionHandler;
    }

    /**
     * This method is used to process received packet and generate a new one
     *
     * @param receivedPacket
     * @return
     */
    private Packet processReceivedPacket(Packet receivedPacket) {
        // Get request header
        RequestHeader requestHeader = receivedPacket.getRequestHeader();
        if (requestHeader == null) {
            throw new DemoException("Request header is null");
        }
        // Get the operation type from request header
        int operationType = requestHeader.getOperationType();
        // Perform business processing according to the operation type
        RequestBody requestBody = receivedPacket.getRequestBody();
        ResponseBody responseBody;
        switch (operationType) {
            case OperationType.TRANSMIT_DATA:
                // Get request body
                TransmitDataRequestBody transmitDataRequestBody = (TransmitDataRequestBody) requestBody;
                // Perform business processing here
                LOG.info("[Business] | Server completes processing data | data type: {} | data: {}", transmitDataRequestBody.getDataType(), transmitDataRequestBody.getData());
                // Create a corresponding response body
                responseBody = TransmitDataResponseBody.create(true);
                break;
            case OperationType.SNOWFLAKE_ID:
                // Get request body
                SnowflakeIdRequestBody snowflakeIdRequestBody = (SnowflakeIdRequestBody) requestBody;
                // Perform business processing here
                SnowFlakeIdWorker snowFlakeIdWorker = SnowFlakeIdWorkerFactory.getWorker(snowflakeIdRequestBody.getAppCode());
                if (snowFlakeIdWorker == null) {
                    LOG.error("Snowflake ID worker is null");
                }
                long snowflakeId = snowFlakeIdWorker.getNextId();
                LOG.info("[Business] | Server completes processing data | app code: {}", snowflakeIdRequestBody.getAppCode());
                // Create a corresponding response body
                responseBody = SnowflakeIdResponseBody.create(snowflakeId);
                break;
            default:
                throw new DemoException("Operation type is unknown");
        }
        // Get the connection ID from request header
        String connectionId = requestHeader.getConnectionId();
        // Create a response header for the packet to return
        ResponseHeader responseHeader = ResponseHeader.create(connectionId, operationType);
        // Return a new packet to send to client
        Packet packet = Packet.create(responseHeader, responseBody);
        LOG.info("[Process] | Server generates a new packet to send | packet: {}", packet);
        return packet;
    }

    public static class Attachment {

        private AioDemoDuplexServerSocket serverSocket;
        private AsynchronousSocketChannel acceptedSocketChannel;
        public ByteBuffer readByteBufferOfHeader;
        public ByteBuffer readByteBufferOfBody;

        private Attachment(AioDemoDuplexServerSocket serverSocket, AsynchronousSocketChannel acceptedSocketChannel, ByteBuffer byteBufferOfHeader, ByteBuffer byteBufferOfBody) {
            this.serverSocket = serverSocket;
            this.acceptedSocketChannel = acceptedSocketChannel;
            this.readByteBufferOfHeader = byteBufferOfHeader;
            this.readByteBufferOfBody = byteBufferOfBody;
        }

        public static Attachment create(AioDemoDuplexServerSocket serverSocket, AsynchronousSocketChannel acceptedSocketChannel, ByteBuffer byteBufferOfHeader, ByteBuffer byteBufferOfBody) {
            return new Attachment(serverSocket, acceptedSocketChannel, byteBufferOfHeader, byteBufferOfBody);
        }

        public AioDemoDuplexServerSocket getServerSocket() {
            return serverSocket;
        }

        public AsynchronousSocketChannel getAcceptedSocketChannel() {
            return acceptedSocketChannel;
        }

        public ByteBuffer getReadByteBufferOfHeader() {
            return readByteBufferOfHeader;
        }

        public ByteBuffer getReadByteBufferOfBody() {
            return readByteBufferOfBody;
        }
    }
}
