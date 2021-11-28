package com.demo.haima.fundamental.client.simplex.nonblocking.socket;

import com.demo.haima.fundamental.client.simplex.nonblocking.SerdesSimplexClient;
import com.demo.haima.fundamental.utils.data.network.definition.ByteBufferType;
import com.demo.haima.fundamental.utils.data.network.packet.Packet;
import com.demo.haima.fundamental.utils.auxiliary.ContainerRunner;
import com.demo.haima.fundamental.utils.state.client.nonblocking.ClientSocketState;
import com.demo.haima.fundamental.utils.state.client.nonblocking.ClientState;
import com.demo.haima.fundamental.utils.state.packet.PacketProcessState;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.SocketAddress;
import java.net.SocketException;
import java.nio.ByteBuffer;
import java.nio.channels.ClosedByInterruptException;
import java.nio.channels.ClosedChannelException;
import java.nio.channels.SocketChannel;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;

/**
 * @author Vince Yuan
 * @date 2021/11/21
 */
public class NioSerdesSimplexClientSocket extends ContainerRunner implements Runnable, SerdesSimplexClientSocket {

    private static final Logger LOG = LoggerFactory.getLogger(NioSerdesSimplexClientSocket.class);

    private SerdesSimplexClient client;
    private ContainerRunner containerRunner;
    private SocketAddress serverAddressToConnect;

    private ClientSocketState clientSocketState;
    public ConcurrentLinkedQueue<Packet> outgoingPacketQueue;
    public ConcurrentHashMap<String, Packet> connectionIdAndProcessingPacketMap;

    public NioSerdesSimplexClientSocket(SerdesSimplexClient client, SocketAddress serverAddressToConnect) {
        try {
            this.client = client;
            containerRunner = new ContainerRunner(this, "ClientSocket");
            this.serverAddressToConnect = serverAddressToConnect;
            outgoingPacketQueue = new ConcurrentLinkedQueue<>();
            connectionIdAndProcessingPacketMap = new ConcurrentHashMap<>();
            this.client.setClientState(ClientState.INITIAL);
            setClientSocketState(ClientSocketState.INITIAL);
        } catch (Throwable t) {
            handleInitializingThrowable(t);
        }
    }

    @Override
    public void run() {
        client.setClientState(ClientState.RUNNING);
        while (client.getClientState() == ClientState.RUNNING) {
            try {
                // Wait until any packet is available
                // Packet packetToSend = outgoingPacketQueue.take();
                Packet packetToSend = outgoingPacketQueue.poll();
                if (packetToSend == null) {
                    continue;
                }
                packetToSend.setPacketProcessState(PacketProcessState.PROCESSING);

                // Keep track of this packet that is under processing
                connectionIdAndProcessingPacketMap.put(packetToSend.getRequestHeader().getConnectionId(), packetToSend);
                LOG.info("[Process] | Packet is waiting for being processed | packet: {}", packetToSend);

                // Set up a client socket channel
                SocketChannel clientSocketChannel = createSocketChannel();
                if (clientSocketChannel == null) {
                    continue;
                }
                configureSocketChannel(clientSocketChannel);
                connectToServer(clientSocketChannel);

                // Wait until the client socket channel is connected to server
                while (!clientSocketChannel.finishConnect()) { }
                LOG.info("Client socket is connected to {}", serverAddressToConnect);
                setClientSocketState(ClientSocketState.CONNECTED);
                logSocketChannelInfo(clientSocketChannel);

                // Get the byte buffer from packet
                ByteBuffer byteBuffer = packetToSend.createByteBufferOnClient(ByteBufferType.DIRECT);
                // Send the byte buffer to server
                int numberOfBytesWritten = clientSocketChannel.write(byteBuffer);
                if (numberOfBytesWritten < 0) {
                    continue;
                }
                LOG.info("[Data] | Client writes bytes to server {} | bytes: {}", serverAddressToConnect, numberOfBytesWritten);
                LOG.info("[Data] | Client writes packet to server {} | packet: {}", serverAddressToConnect, packetToSend);

                // Mark the packet as processed
                packetToSend.setPacketProcessState(PacketProcessState.PROCESSED);

                // Remove the track of this packet that has been processed
                connectionIdAndProcessingPacketMap.remove(packetToSend.getRequestHeader().getConnectionId());
                LOG.info("[Process] | Packet is processed successfully | packet: {}", packetToSend);

                // Close the socket channel
                closeSocketChannel(clientSocketChannel);
            } catch (Throwable t) {
                handleRunningThrowable(t);
            }
        }
    }

    @Override
    public void startSocket() {
        try {
            containerRunner.start();
        } catch (Throwable t) {
            handleStartingThrowable(t);
        }
    }

    @Override
    public void closeSocket() {
        try {
            while (outgoingPacketQueue.size() > 0 || connectionIdAndProcessingPacketMap.size() > 0) { }
            // Here interrupt must be invoked before client state is set to CLOSED,
            // otherwise, server will encounter a CancelledKeyException while processing
            // selected key on connect event, or client will miss out 15% percent
            // of packets that are supposed to be sent to server on top for now
            // containerRunner.interrupt();
            client.setClientState(ClientState.CLOSED);
            containerRunner.join();
        } catch (Throwable t) {
            handleClosingThrowable(t);
        }
    }

    @Override
    public void handleInitializingThrowable(Throwable t) {
        LOG.error("Create socket error", t);
    }

    @Override
    public void handleStartingThrowable(Throwable t) {
        LOG.error("Start socket error", t);
    }

    @Override
    public void handleClosingThrowable(Throwable t) {
        LOG.info("Close socket error", t);
    }

    @Override
    public void handleRunningThrowable(Throwable t) {
        if (t instanceof InterruptedException) {
            // The exception can be ignored here since it interrupts the blocking operation (if it is used)
            // of queue to ensure closing can be proceeded. It may be occasionally raised, since the execution
            // cannot proceed to the blocking operation (if it is used) every time when closing socket is executed
        } else if (t instanceof ClosedChannelException) {
            // This exception can be ignored here since client socket channel may be closed when the execution of
            // connect is going on. Hence, the client needs to be closed, the operation of client socket channel
            // needs to be interrupted for sure. Hence, this exception is OK here in this scenario
        } else {
            LOG.info("Process socket error", t);
        }
    }

    @Override
    public void submit(Packet packet) {
        try {
            outgoingPacketQueue.offer(packet);
        } catch (Throwable t) {
            LOG.error("Submit packet error", t);
        }
    }

    @Override
    public SocketChannel createSocketChannel() {
        try {
            SocketChannel clientSocketChannel = SocketChannel.open();
            LOG.info("Client socket channel is opened: {}", clientSocketChannel);
            return clientSocketChannel;
        } catch (Throwable t) {
            handleCreateSocketChannelThrowable(t);
            return null;
        }
    }

    @Override
    public void configureSocketChannel(SocketChannel socketChannel) {
        try {
            socketChannel.configureBlocking(false);
            LOG.info("Client socket channel is configured to {}", getSelectableChannelBlockingMode(socketChannel));
            socketChannel.socket().setTcpNoDelay(true);
            // set socket linger to false, so that socket close does not block
            socketChannel.socket().setSoLinger(false, -1);
        } catch (Throwable t) {
            handleConfigureSocketChannelThrowable(t);
        }
    }

    @Override
    public void connectToServer(SocketChannel socketChannel) {
        try {
            socketChannel.connect(serverAddressToConnect);
            LOG.info("Client socket channel is connecting to {}", serverAddressToConnect);
            setClientSocketState(ClientSocketState.CONNECTING);
        } catch (Throwable t) {
            handleConnectToServerThrowable(t);
        }
    }

    @Override
    public void closeSocketChannel(SocketChannel socketChannel) {
        try {
            if (socketChannel.socket().isClosed()) {
                return;
            }
            if (socketChannel.socket().isConnected()) {
                socketChannel.socket().shutdownInput();
                socketChannel.socket().shutdownOutput();
            }
            socketChannel.socket().close();
            socketChannel.close();
            setClientSocketState(ClientSocketState.CLOSED);
            LOG.info("Client socket channel is closed");
        } catch (Throwable t) {
            handleCloseSocketChannelThrowable(t);
        }
    }

    @Override
    public void handleCreateSocketChannelThrowable(Throwable t) {
        LOG.error("create socket channel error", t);
    }

    @Override
    public void handleConfigureSocketChannelThrowable(Throwable t) {
        if (t instanceof SocketException) {
            String message = t.getMessage();
            if ("Invalid argument: no further information".equals(message)) {
                LOG.error("Occasionally occur, reason unknown yet, but it does not affect server reading data", t);
            } else {
                LOG.error("Configure socket error", t);
            }
        } else {
            LOG.error("Configure socket error", t);
        }
    }

    @Override
    public void handleConnectToServerThrowable(Throwable t) {
        if (t instanceof ClosedByInterruptException) {
            LOG.info("This exception is raised while the interruption of happens during connecting. This " +
                    "exception will miss out a packet to process. This exception is occasionally raised", t);
        } else {
            LOG.error("connect to server error", t);
        }
    }

    @Override
    public void handleCloseSocketChannelThrowable(Throwable t) {
        LOG.error("handle close socket channel error", t);
    }

    /************************************** State Machine **************************************/

    public void setClientSocketState(ClientSocketState clientSocketState) {
        this.clientSocketState = clientSocketState;
        LOG.info("Client socket state is set to {}", clientSocketState);
    }

    public ClientSocketState getClientSocketState() {
        return clientSocketState;
    }
}
