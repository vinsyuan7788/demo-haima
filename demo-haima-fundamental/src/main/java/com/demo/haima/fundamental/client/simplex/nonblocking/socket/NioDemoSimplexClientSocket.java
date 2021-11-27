package com.demo.haima.fundamental.client.simplex.nonblocking.socket;

import com.demo.haima.fundamental.client.simplex.nonblocking.DemoSimplexClient;
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
public class NioDemoSimplexClientSocket extends ContainerRunner implements Runnable, DemoSimplexClientSocket {

    private static final Logger LOG = LoggerFactory.getLogger(NioDemoSimplexClientSocket.class);

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

    private DemoSimplexClient client;
    private ContainerRunner containerRunner;
    private SocketAddress serverAddressToConnect;
    private Selector selector;
    private SelectionKey registeredKey;

    private ClientSocketState clientSocketState;
    private ConcurrentLinkedQueue<Packet> outgoingPacketQueue;
    private ConcurrentHashMap<String, Packet> connectionIdAndProcessingPacketMap;

    public NioDemoSimplexClientSocket(DemoSimplexClient client, SocketAddress serverAddressToConnect) {
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
            closeSocketChannel((SocketChannel) registeredKey.channel());
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
    public void submit(Packet packet) {
        try {
            outgoingPacketQueue.offer(packet);
        } catch (Throwable t) {
            LOG.error("Submit packet error", t);
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
        // Do nothing here
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
        SelectionKey registeredKey = clientSocketChannel.register(selector, SelectionKey.OP_WRITE);
        logSelectionKeyInfo(selectedKey, "write", registeredKey, "write");

        // Mark the packet as processed
        packetToSend.setPacketProcessState(PacketProcessState.PROCESSED);

        // Remove the track of this packet that has been processed
        connectionIdAndProcessingPacketMap.remove(packetToSend.getRequestHeader().getConnectionId());
        LOG.info("[Process] | Packet is processed successfully | packet: {}", packetToSend);

        // Close the client socket channel
//        selectedKey.cancel();
//        closeSocketChannel(clientSocketChannel);
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
