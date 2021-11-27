package com.demo.haima.fundamental.client.duplex.nonblocking.socket;

import com.demo.haima.fundamental.client.duplex.nonblocking.DemoDuplexClient;
import com.demo.haima.fundamental.utils.auxiliary.ContainerRunner;
import com.demo.haima.fundamental.utils.data.network.definition.ByteBufferType;
import com.demo.haima.fundamental.utils.data.network.definition.OperationType;
import com.demo.haima.fundamental.utils.data.network.packet.Packet;
import com.demo.haima.fundamental.utils.data.network.response.body.ResponseBody;
import com.demo.haima.fundamental.utils.data.network.response.body.SnowflakeIdResponseBody;
import com.demo.haima.fundamental.utils.data.network.response.body.TransmitDataResponseBody;
import com.demo.haima.fundamental.utils.data.network.response.header.ResponseHeader;
import com.demo.haima.fundamental.utils.exception.DemoException;
import com.demo.haima.fundamental.utils.state.client.nonblocking.ClientSocketState;
import com.demo.haima.fundamental.utils.state.client.nonblocking.ClientState;
import com.demo.haima.fundamental.utils.state.packet.PacketProcessState;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.SocketAddress;
import java.net.SocketException;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.SocketChannel;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;

/**
 * @author Vince Yuan
 * @date 2021/11/21
 */
public class NioDemoDuplexClientSocket extends ContainerRunner implements Runnable, DemoDuplexClientSocket {

    private static final Logger LOG = LoggerFactory.getLogger(NioDemoDuplexClientSocket.class);

    static {
        /**
         * this is to avoid the jvm bug:
         * NullPointerException in Selector.open()
         * http://bugs.sun.com/view_bug.do?bug_id=6427854
         */
        try {
            Selector.open().close();
        } catch (Exception e) {
            LOG.error("Selector failed to open", e);
        }
    }

    private DemoDuplexClient client;
    private ContainerRunner containerRunner;
    private SocketAddress serverAddressToConnect;
    private Selector selector;
    private SelectionKey registeredKey;

    private ClientSocketState clientSocketState;
    private ConcurrentLinkedQueue<Packet> outgoingPacketQueue;
    private ConcurrentHashMap<String, Packet> connectionIdAndProcessingPacketMap;

    public NioDemoDuplexClientSocket(DemoDuplexClient client, SocketAddress serverAddressToConnect) {
        try {
            this.client = client;
            containerRunner = new ContainerRunner(this, "ClientSocket");
            this.serverAddressToConnect = serverAddressToConnect;
            outgoingPacketQueue = new ConcurrentLinkedQueue<>();
            connectionIdAndProcessingPacketMap = new ConcurrentHashMap<>();
            this.client.setClientState(ClientState.INITIAL);

            setClientSocketState(ClientSocketState.INITIAL);
            this.selector = Selector.open();
            LOG.info("Selector is opened: {}", selector);
        } catch (Throwable t) {
            handleInitializingThrowable(t);
        }
    }

    @Override
    public void run() {
        client.setClientState(ClientState.RUNNING);
        while (client.getClientState() == ClientState.RUNNING) {
            try {
                // Socket channel connects to server if it is not connected yet
                if (getClientSocketState() == ClientSocketState.INITIAL
                        || getClientSocketState() == ClientSocketState.CONNECTING) {
                    // Set up a client socket channel
                    SocketChannel clientSocketChannel = createSocketChannel();
                    if (clientSocketChannel == null) {
                        return;
                    }
                    configureSocketChannel(clientSocketChannel);
                    SelectionKey registeredKey = clientSocketChannel.register(selector, SelectionKey.OP_CONNECT);
                    LOG.info("Client socket channel is registered to selector with interest of connect event | registered key: {}", registeredKey);
                    connectToServer(clientSocketChannel);
                }

                // Process the socket channel with NIO
                selectKeys(selector);
                Set<SelectionKey> selectedKeys = getSelectedKeys(selector);
                processSelectedKeys(selectedKeys);
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
            client.setClientState(ClientState.CLOSED);
            containerRunner.join();
            // Cancel the registered key and close the client socket channel
            registeredKey.cancel();
            SocketChannel clientSocketChannel = (SocketChannel) registeredKey.channel();
            closeSocketChannel(clientSocketChannel);
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
            // The exception can be ignored here since it interrupts the blocking operation of queue to ensure closing can be proceeded
        } else {
            LOG.info("Process socket error", t);
        }
    }

    @Override
    public ResponseBody submit(Packet packet) {
        try {
            outgoingPacketQueue.offer(packet);
            // Wait for the packet to be processed
            synchronized (packet) {
                packet.wait();
            }
            return packet.getResponseBody();
        } catch (Throwable t) {
            throw new DemoException("Submit packet error", t);
        }
    }

    @Override
    public void selectKeys(Selector selector) throws Exception {
        selector.select(1000);
    }

    @Override
    public Set<SelectionKey> getSelectedKeys(Selector selector) {
        return selector.selectedKeys();
    }

    @Override
    public void processSelectedKeys(Set<SelectionKey> selectedKeys) throws Exception {
        for (SelectionKey selectedKey : selectedKeys) {
            if (selectedKey.isAcceptable()) {
                processAcceptEvent(selectedKey);
            } else if (selectedKey.isConnectable()) {
                processConnectableEvent(selectedKey);
            } else if (selectedKey.isReadable()) {
                processReadEvent(selectedKey);
            } else if (selectedKey.isWritable()) {
                processWritableEvent(selectedKey);
            }
        }
        selectedKeys.clear();
    }

    @Override
    public void processAcceptEvent(SelectionKey selectedKey) throws Exception {
        // Do nothing here
    }

    @Override
    public void processConnectableEvent(SelectionKey selectedKey) throws Exception {
        // Get the client socket channel that finishes connecting to server
        SocketChannel clientSocketChannel = (SocketChannel) selectedKey.channel();
        if (clientSocketChannel == null) {
            return;
        }
        if (!clientSocketChannel.finishConnect()) {
            return;
        }
        LOG.info("Client socket is connected to {}", serverAddressToConnect);
        setClientSocketState(ClientSocketState.CONNECTED);
        logSocketChannelInfo(clientSocketChannel);

        // Register the accepted socket channel to selector
        registeredKey = clientSocketChannel.register(selector, SelectionKey.OP_WRITE);
        logSelectionKeyInfo(selectedKey, "connect", registeredKey, "write");
    }

    @Override
    public void processReadEvent(SelectionKey selectedKey) throws Exception {
        // Get the client socket channel
        SocketChannel clientSocketChannel = (SocketChannel) selectedKey.channel();
        if (clientSocketChannel == null) {
            return;
        }

        // Scatter-read the data sent from server through the channel into byte buffers
        ByteBuffer byteBufferOfHeader = ByteBuffer.allocateDirect(42);
        ByteBuffer byteBufferOfBody = ByteBuffer.allocateDirect(1024);
        ByteBuffer[] byteBuffers = new ByteBuffer[] { byteBufferOfHeader, byteBufferOfBody };
        long numberOfBytesRead = clientSocketChannel.read(byteBuffers);
        if (numberOfBytesRead < 0) {
            return;
        }
        LOG.info("[Data] | Client reads bytes from server {} | bytes: {}", serverAddressToConnect, numberOfBytesRead);

        // Read the data from the byte buffers
        Packet packet = Packet.readOnClient(byteBufferOfHeader, byteBufferOfBody);
        LOG.info("[Data] | Client reads packet from server {} | packet: {}", serverAddressToConnect, packet);

        // Process received packet
        processReceivedPacket(packet);

        // Register the accepted socket channel to selector
        SelectionKey registeredKey = clientSocketChannel.register(selector, SelectionKey.OP_WRITE);
        logSelectionKeyInfo(selectedKey, "read", registeredKey, "write");

        // Close the client socket channel
//        selectedKey.cancel();
//        closeSocketChannel(clientSocketChannel);
    }

    @Override
    public void processWritableEvent(SelectionKey selectedKey) throws Exception {
        // Get the client socket channel
        SocketChannel clientSocketChannel = (SocketChannel) selectedKey.channel();
        if (clientSocketChannel == null) {
            return;
        }

        // Get the packet to send
        Packet packetToSend = outgoingPacketQueue.poll();
        if (packetToSend == null) {
            // Close the socket channel
//            closeSocketChannel(clientSocketChannel);
            return;
        }
        packetToSend.setPacketProcessState(PacketProcessState.PROCESSING);

        // Keep track of this packet that is under processing
        connectionIdAndProcessingPacketMap.put(packetToSend.getRequestHeader().getConnectionId(), packetToSend);
        LOG.info("[Process] | Packet is waiting for being processed | packet: {}", packetToSend);

        // Get the byte buffers from packet
        ByteBuffer[] byteBuffers = packetToSend.getByteBuffersOnClient(ByteBufferType.DIRECT);
        // Gather-write the byte buffers to server
        long numberOfBytesWritten = clientSocketChannel.write(byteBuffers);
        if (numberOfBytesWritten < 0) {
            return;
        }
        LOG.info("[Data] | Client writes bytes to server {} | bytes: {}", serverAddressToConnect, numberOfBytesWritten);
        LOG.info("[Data] | Client writes packet to server {} | packet: {}", serverAddressToConnect, packetToSend);

        // Register the accepted socket channel to selector
        SelectionKey registeredKey = clientSocketChannel.register(selector, SelectionKey.OP_READ);
        logSelectionKeyInfo(selectedKey, "write", registeredKey, "read");
    }

    @Override
    public void processReceivedPacket(Packet receivedPacket) {
        // Get the response header
        ResponseHeader responseHeader = receivedPacket.getResponseHeader();
        if (responseHeader == null) {
            throw new DemoException("Response header is null");
        }
        // Get the connection ID from response header
        String connectionId = responseHeader.getConnectionId();
        // Check if received packet is the one that needs to be processed
        Packet processingPacket = connectionIdAndProcessingPacketMap.get(connectionId);
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
        connectionIdAndProcessingPacketMap.remove(connectionId);
        LOG.info("[Process] | Packet is processed successfully | packet: {}", processingPacket);
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
        LOG.error("connect to server error", t);
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
