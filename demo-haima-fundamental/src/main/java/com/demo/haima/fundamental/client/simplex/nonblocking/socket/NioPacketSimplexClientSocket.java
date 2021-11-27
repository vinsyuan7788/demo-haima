package com.demo.haima.fundamental.client.simplex.nonblocking.socket;

import com.demo.haima.fundamental.utils.data.network.base.packet.Packet;
import com.demo.haima.fundamental.utils.auxiliary.ContainerRunner;
import com.demo.haima.fundamental.utils.state.client.nonblocking.ClientSocketState;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.SocketAddress;
import java.net.SocketException;
import java.nio.ByteBuffer;
import java.nio.channels.ClosedChannelException;
import java.nio.channels.SocketChannel;
import java.util.concurrent.LinkedBlockingQueue;

/**
 * @author Vince Yuan
 * @date 2021/11/20
 */
public class NioPacketSimplexClientSocket extends ContainerRunner implements Runnable, PacketSimplexClientSocket {

    private static final Logger LOG = LoggerFactory.getLogger(NioPacketSimplexClientSocket.class);

    private ContainerRunner containerRunner;
    private SocketChannel clientSocketChannel;
    private SocketAddress serverAddressToConnect;

    private ClientSocketState clientSocketState;
    private LinkedBlockingQueue<Packet<?>> outgoingPacketQueue;

    public NioPacketSimplexClientSocket(SocketAddress serverAddressToConnect) {
        try {
            containerRunner = new ContainerRunner(this, "ClientSocket");
            outgoingPacketQueue = new LinkedBlockingQueue<>();

            setClientSocketState(ClientSocketState.INITIAL);
            clientSocketChannel = SocketChannel.open();
            LOG.info("Client socket channel is opened: {}", clientSocketChannel);
            clientSocketChannel.configureBlocking(false);
            LOG.info("Client socket channel is configured to {}", getSelectableChannelBlockingMode(clientSocketChannel));
            clientSocketChannel.socket().setTcpNoDelay(true);
            // set socket linger to false, so that socket close does not block
            clientSocketChannel.socket().setSoLinger(false, -1);

            this.serverAddressToConnect = serverAddressToConnect;
            clientSocketChannel.connect(serverAddressToConnect);
            LOG.info("Client socket is connecting to {}", serverAddressToConnect);
            setClientSocketState(ClientSocketState.CONNECTING);
        } catch (Throwable t) {
            handleInitializingThrowable(t);
        }
    }

    @Override
    public void run() {
        try {
            // Wait until the client socket is connected to server
            while (!clientSocketChannel.finishConnect()) { }
            LOG.info("Client socket is connected to {}", serverAddressToConnect);
            setClientSocketState(ClientSocketState.CONNECTED);
            logSocketChannelInfo(clientSocketChannel);

            // Wait until the packet is available
            Packet<?> packetToSend = outgoingPacketQueue.take();

            // Get the byte buffer from packet
            ByteBuffer byteBuffer = packetToSend.getByteBuffer();
            // Send the byte buffer to server
            int numberOfBytesWritten = clientSocketChannel.write(byteBuffer);
            if (numberOfBytesWritten < 0) {
                return;
            }
            LOG.info("[Data] | Client writes bytes to server {} | bytes: {}", serverAddressToConnect, numberOfBytesWritten);
            LOG.info("[Data] | Client writes packet to server {} | packet: {}", serverAddressToConnect, packetToSend);
        } catch (Throwable t) {
            handleRunningThrowable(t);
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
            // To ensure the communication with server can be done
            // before closing resources (e.g., socket channel, socket)
            containerRunner.join();
            // Close socket channel and socket
            if (clientSocketChannel.socket().isClosed()) {
                return;
            }
            if (clientSocketChannel.socket().isConnected()) {
                clientSocketChannel.socket().shutdownInput();
                clientSocketChannel.socket().shutdownOutput();
            }
            clientSocketChannel.socket().close();
            clientSocketChannel.close();
            setClientSocketState(ClientSocketState.CLOSED);
            LOG.info("Client socket channel is closed");
        } catch (Throwable t) {
            handleClosingThrowable(t);
        }
    }

    @Override
    public void handleInitializingThrowable(Throwable t) {
        if (t instanceof SocketException) {
            String message = t.getMessage();
            if ("Invalid argument: no further information".equals(message)) {
                LOG.error("Occasionally occur, reason unknown yet, but it does not affect server reading data", t);
            } else {
                LOG.error("Create socket error", t);
            }
        } else {
            LOG.error("Create socket error", t);
        }
    }

    @Override
    public void handleStartingThrowable(Throwable t) {
        LOG.error("Start socket error", t);
    }

    @Override
    public void handleClosingThrowable(Throwable t) {
        LOG.error("Close socket error", t);
    }

    @Override
    public void handleRunningThrowable(Throwable t) {
        if (t instanceof ClosedChannelException) {
            LOG.info("Please ensure the communication with server is done before closing the channel");
        } else {
            LOG.info("Process socket error", t);
        }
    }

    @Override
    public void submit(Packet<?> packet) {
        try {
            outgoingPacketQueue.offer(packet);
        } catch (Throwable t) {
            LOG.error("Submit data error", t);
        }
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
