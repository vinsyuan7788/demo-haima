package com.demo.haima.fundamental.server.duplex.asynchronous.socket.handler;

import com.demo.haima.fundamental.server.duplex.asynchronous.socket.AioDemoDuplexServerSocket;
import com.demo.haima.fundamental.server.duplex.asynchronous.socket.handler.AioDemoDuplexServerSocketWriteCompletionHandler.Attachment;
import com.demo.haima.fundamental.utils.auxiliary.CompletionHandlerHelper;
import com.demo.haima.fundamental.utils.data.network.packet.Packet;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.ByteBuffer;
import java.nio.channels.AsynchronousSocketChannel;
import java.nio.channels.CompletionHandler;

/**
 * @author Vince Yuan
 * @date 2021/11/24
 */
public class AioDemoDuplexServerSocketWriteCompletionHandler extends CompletionHandlerHelper implements CompletionHandler<Integer, Attachment> {

    private static final Logger LOG = LoggerFactory.getLogger(AioDemoDuplexServerSocketWriteCompletionHandler.class);

    private AioDemoDuplexServerSocketReadCompletionHandler readCompletionHandler;

    @Override
    public void completed(Integer numberOfBytesWritten, AioDemoDuplexServerSocketWriteCompletionHandler.Attachment attachment) {
        try {
            // Get content from attachment
            AioDemoDuplexServerSocket serverSocket = attachment.getServerSocket();
            AsynchronousSocketChannel acceptedSocketChannel = attachment.getAcceptedSocketChannel();
            Packet packetToSend = attachment.getPacketToSend();

            // check write event completion result
            if (numberOfBytesWritten < 0) {
                return;
            }
            LOG.info("[Data] | Server writes bytes to client {} | bytes: {}", acceptedSocketChannel.getRemoteAddress(), numberOfBytesWritten);
            LOG.info("[Data] | Server writes packet to client {} | packet: {}", acceptedSocketChannel.getRemoteAddress(), packetToSend);

            // Read the data sent from client through the channel into a byte buffer
            ByteBuffer byteBuffer = ByteBuffer.allocateDirect(1024);
            acceptedSocketChannel.read(byteBuffer, AioDemoDuplexServerSocketReadCompletionHandler.Attachment.create(serverSocket, acceptedSocketChannel, byteBuffer), readCompletionHandler);
        } catch (Throwable t) {
            handleRunningThrowable(t);
        }
    }

    @Override
    public void failed(Throwable t, AioDemoDuplexServerSocketWriteCompletionHandler.Attachment attachment) {
        LOG.error("Write event error", t);
    }

    public void setReadCompletionHandler(AioDemoDuplexServerSocketReadCompletionHandler readCompletionHandler) {
        this.readCompletionHandler = readCompletionHandler;
    }

    public static class Attachment {

        private AioDemoDuplexServerSocket serverSocket;
        private AsynchronousSocketChannel acceptedSocketChannel;
        private Packet packetToSend;

        private Attachment(AioDemoDuplexServerSocket serverSocket, AsynchronousSocketChannel acceptedSocketChannel, Packet packetToSend) {
            this.serverSocket = serverSocket;
            this.acceptedSocketChannel = acceptedSocketChannel;
            this.packetToSend = packetToSend;
        }

        public static Attachment create(AioDemoDuplexServerSocket serverSocket, AsynchronousSocketChannel acceptedSocketChannel, Packet packetToSend) {
            return new Attachment(serverSocket, acceptedSocketChannel, packetToSend);
        }

        public AioDemoDuplexServerSocket getServerSocket() {
            return serverSocket;
        }

        public AsynchronousSocketChannel getAcceptedSocketChannel() {
            return acceptedSocketChannel;
        }

        public Packet getPacketToSend() {
            return packetToSend;
        }
    }
}
