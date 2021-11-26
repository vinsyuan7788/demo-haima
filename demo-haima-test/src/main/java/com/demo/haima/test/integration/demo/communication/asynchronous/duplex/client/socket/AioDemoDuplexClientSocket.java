package com.demo.haima.test.integration.demo.communication.asynchronous.duplex.client.socket;

import com.demo.haima.test.integration.demo.communication.asynchronous.duplex.client.DemoDuplexClient;
import com.demo.haima.test.integration.demo.communication.asynchronous.duplex.client.socket.handler.AioDemoDuplexClientSocketConnectCompletionHandler;
import com.demo.haima.test.integration.demo.communication.asynchronous.duplex.client.socket.handler.AioDemoDuplexClientSocketReadCompletionHandler;
import com.demo.haima.test.integration.demo.communication.asynchronous.duplex.client.socket.handler.AioDemoDuplexClientSocketWriteCompletionHandler;
import com.demo.haima.test.integration.demo.utils.auxiliary.ContainerRunner;
import com.demo.haima.test.integration.demo.utils.data.network.packet.Packet;
import com.demo.haima.test.integration.demo.utils.data.network.response.body.ResponseBody;
import com.demo.haima.test.integration.demo.utils.exception.DemoException;
import com.demo.haima.test.integration.demo.utils.state.client.aio.ClientSocketState;
import com.demo.haima.test.integration.demo.utils.state.client.aio.ClientState;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.SocketAddress;
import java.nio.channels.AsynchronousChannelGroup;
import java.nio.channels.AsynchronousSocketChannel;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * @author Vince Yuan
 * @date 2021/11/24
 */
public class AioDemoDuplexClientSocket extends ContainerRunner implements Runnable, DemoDuplexClientSocket {

    private static final Logger LOG = LoggerFactory.getLogger(AioDemoDuplexClientSocket.class);

    private DemoDuplexClient client;
    private ContainerRunner containerRunner;
    private SocketAddress serverAddressToConnect;
    private AsynchronousChannelGroup channelGroup;
    private AsynchronousSocketChannel clientSocketChannel;
    private AioDemoDuplexClientSocketConnectCompletionHandler connectCompletionHandler;

    private ClientSocketState clientSocketState;
    private ConcurrentLinkedQueue<Packet> outgoingPacketQueue;
    private ConcurrentHashMap<String, Packet> connectionIdAndProcessingPacketMap;

    private AtomicInteger connectingTracker;

    public AioDemoDuplexClientSocket(DemoDuplexClient client, SocketAddress serverAddressToConnect) {
        try {
            this.client = client;
            containerRunner = new ContainerRunner(this, "ClientSocket");
            this.serverAddressToConnect = serverAddressToConnect;
            outgoingPacketQueue = new ConcurrentLinkedQueue<>();
            connectionIdAndProcessingPacketMap = new ConcurrentHashMap<>();
            connectCompletionHandler = buildConnectCompletionHandler();
            connectingTracker = new AtomicInteger(0);
            this.client.setClientState(ClientState.INITIAL);

            setClientSocketState(ClientSocketState.INITIAL);
            ExecutorService defaultCachedThreadPool = getDefaultCachedThreadPool("AIO-ClientSocket-EventHandler");
            channelGroup = AsynchronousChannelGroup.withThreadPool(defaultCachedThreadPool);
            clientSocketChannel = AsynchronousSocketChannel.open(channelGroup);
        } catch (Throwable t) {
            handleInitializingThrowable(t);
        }
    }

    @Override
    public void run() {
        client.setClientState(ClientState.RUNNING);
        while (clientSocketChannel.isOpen()) {
            try {
                if (connectingTracker.get() > 0) { continue; }
                clientSocketChannel.connect(serverAddressToConnect, AioDemoDuplexClientSocketConnectCompletionHandler.Attachment.create(this), connectCompletionHandler);
                connectingTracker.incrementAndGet();
                setClientSocketState(ClientSocketState.CONNECTING);
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
            closeSocketChannel(clientSocketChannel);
            containerRunner.join();
            client.setClientState(ClientState.CLOSED);
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
        LOG.info("Process socket error", t);
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
    public void closeSocketChannel(AsynchronousSocketChannel socketChannel) {
        try {
            if (!socketChannel.isOpen()) {
                return;
            }
            socketChannel.close();
            setClientSocketState(ClientSocketState.CLOSED);
            LOG.info("Client socket channel is closed");
        } catch (Throwable t) {
            handleCloseSocketChannelThrowable(t);
        }
    }

    @Override
    public void handleCloseSocketChannelThrowable(Throwable t) {
        LOG.error("handle close socket channel error", t);
    }

    /************************************** Getter & Setter **************************************/

    public AsynchronousSocketChannel getClientSocketChannel() {
        return clientSocketChannel;
    }

    public SocketAddress getServerAddressToConnect() {
        return serverAddressToConnect;
    }

    public ConcurrentLinkedQueue<Packet> getOutgoingPacketQueue() {
        return outgoingPacketQueue;
    }

    public ConcurrentHashMap<String, Packet> getConnectionIdAndProcessingPacketMap() {
        return connectionIdAndProcessingPacketMap;
    }

    /************************************** State Machine **************************************/

    public synchronized void setClientSocketState(ClientSocketState clientSocketState) {
        this.clientSocketState = clientSocketState;
        LOG.info("Client socket state is set to {}", clientSocketState);
    }

    public ClientSocketState getClientSocketState() {
        return clientSocketState;
    }

    /************************************** Utility Method **************************************/

    private AioDemoDuplexClientSocketConnectCompletionHandler buildConnectCompletionHandler() {
        AioDemoDuplexClientSocketWriteCompletionHandler writeCompletionHandler = new AioDemoDuplexClientSocketWriteCompletionHandler();
        AioDemoDuplexClientSocketReadCompletionHandler readCompletionHandler = new AioDemoDuplexClientSocketReadCompletionHandler();
        writeCompletionHandler.setReadCompletionHandler(readCompletionHandler);
        readCompletionHandler.setWriteCompletionHandler(writeCompletionHandler);
        return new AioDemoDuplexClientSocketConnectCompletionHandler(writeCompletionHandler);
    }
}
