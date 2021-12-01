package com.demo.haima.fundamental.server.duplex.nonblocking.socket;

import com.demo.haima.fundamental.server.duplex.nonblocking.DemoDuplexServer;
import com.demo.haima.fundamental.utility.business.snowflake.SnowFlakeIdWorker;
import com.demo.haima.fundamental.utility.business.snowflake.factory.SnowFlakeIdWorkerFactory;
import com.demo.haima.fundamental.utility.data.network.definition.ByteBufferType;
import com.demo.haima.fundamental.utility.data.network.definition.OperationType;
import com.demo.haima.fundamental.utility.data.network.packet.Packet;
import com.demo.haima.fundamental.utility.data.network.request.body.RequestBody;
import com.demo.haima.fundamental.utility.data.network.request.body.SnowflakeIdRequestBody;
import com.demo.haima.fundamental.utility.data.network.request.body.TransmitDataRequestBody;
import com.demo.haima.fundamental.utility.data.network.request.header.RequestHeader;
import com.demo.haima.fundamental.utility.data.network.response.body.ResponseBody;
import com.demo.haima.fundamental.utility.data.network.response.body.SnowflakeIdResponseBody;
import com.demo.haima.fundamental.utility.data.network.response.body.TransmitDataResponseBody;
import com.demo.haima.fundamental.utility.data.network.response.header.ResponseHeader;
import com.demo.haima.fundamental.utility.exception.DemoException;
import com.demo.haima.fundamental.utility.auxiliary.ContainerRunner;
import com.demo.haima.fundamental.utility.state.server.nonblocking.ServerSocketState;
import com.demo.haima.fundamental.utility.state.server.nonblocking.ServerState;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.ClosedSelectorException;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.util.Set;

/**
 * @author Vince Yuan
 * @date 2021/11/20
 */
public class NioDemoDuplexServerSocket extends ContainerRunner implements Runnable, DemoDuplexServerSocket {

    private static final Logger LOG = LoggerFactory.getLogger(NioDemoDuplexServerSocket.class);

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

    private DemoDuplexServer server;
    private ContainerRunner containerRunner;
    public ServerSocketChannel serverSocketChannel;
    private Selector selector;

    private ServerSocketState serverSocketState;

    public NioDemoDuplexServerSocket(DemoDuplexServer server, int portToBind) {
        try {
            this.server = server;
            this.server.setServerState(ServerState.INITIAL);
            containerRunner = new ContainerRunner(this, "ServerSocket");

            setServerSocketState(ServerSocketState.INITIAL);
            serverSocketChannel = ServerSocketChannel.open();
            LOG.info("Server socket channel is opened: {}", serverSocketChannel);
            serverSocketChannel.configureBlocking(false);
            LOG.info("Server socket channel is configured to {}", getSelectableChannelBlockingMode(serverSocketChannel));
            serverSocketChannel.bind(new InetSocketAddress(portToBind));
            LOG.info("Server socket channel is bound to port: {}", portToBind);
            setServerSocketState(ServerSocketState.BOUND);

            selector = Selector.open();
            LOG.info("Selector is opened: {}", selector);
            SelectionKey registeredKey = serverSocketChannel.register(selector, SelectionKey.OP_ACCEPT);
            LOG.info("Server socket channel is registered to selector with interest of accept event | registered key: {}", registeredKey);
        } catch (Throwable t) {
            handleInitializingThrowable(t);
        }
    }

    @Override
    public void run() {
        server.setServerState(ServerState.RUNNING);
        while (!serverSocketChannel.socket().isClosed()) {
            try {
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
            SnowFlakeIdWorkerFactory.init(5, 10, 2);
            containerRunner.start();
        } catch (Throwable t) {
            handleStartingThrowable(t);
        }
    }

    @Override
    public void closeSocket() {
        try {
            // Close the server socket
            if (serverSocketChannel.socket().isClosed()) {
                return;
            }
            serverSocketChannel.socket().close();
            serverSocketChannel.close();
            // Wait until server socket thread stops running
            containerRunner.join();
            // Close the selector
            selector.close();
            setServerSocketState(ServerSocketState.CLOSED);
            server.setServerState(ServerState.CLOSED);
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
        LOG.error("Close socket error", t);
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
                processConnectEvent(selectedKey);
            } else if (selectedKey.isReadable()) {
                processReadEvent(selectedKey);
            } else if (selectedKey.isWritable()) {
                processWriteEvent(selectedKey);
            }
        }
        selectedKeys.clear();
    }

    @Override
    public void handleRunningThrowable(Throwable t) {
        if (t instanceof ClosedSelectorException) {
            LOG.error("Please ensure current thread stops running before closing the selector", t);
        } else {
            LOG.error("Process socket error", t);
        }
    }

    @Override
    public void processAcceptEvent(SelectionKey selectedKey) throws Exception {
        // Accept the socket channel from client
        ServerSocketChannel serverSocketChannel = (ServerSocketChannel) selectedKey.channel();
        if (serverSocketChannel == null) {
            return;
        }
        logServerSocketChannelInfo(serverSocketChannel);
        SocketChannel acceptedSocketChannel = serverSocketChannel.accept();
        if (acceptedSocketChannel == null) {
            return;
        }
        logSocketChannelInfo(acceptedSocketChannel);

        // Configure the accepted socket
        acceptedSocketChannel.socket().setTcpNoDelay(true);
        // set socket linger to false, so that socket close does not block
        acceptedSocketChannel.socket().setSoLinger(false, -1);

        // Register the accepted socket channel to selector
        acceptedSocketChannel.configureBlocking(false);
        SelectionKey registeredKey = acceptedSocketChannel.register(selector, SelectionKey.OP_READ);
        logSelectionKeyInfo(selectedKey, "accept", registeredKey, "read");
    }

    @Override
    public void processConnectEvent(SelectionKey selectedKey) throws Exception {
        // Do nothing here
    }

    @Override
    public void processReadEvent(SelectionKey selectedKey) throws Exception {
        // Get the accepted socket channel
        SocketChannel acceptedSocketChannel = (SocketChannel) selectedKey.channel();
        if (acceptedSocketChannel == null) {
            return;
        }

        // Scatter-read the data sent from client through the channel into byte buffers
        ByteBuffer byteBufferOfHeader = ByteBuffer.allocateDirect(42);
        ByteBuffer byteBufferOfBody = ByteBuffer.allocateDirect(1024);
        ByteBuffer[] byteBuffers = new ByteBuffer[] { byteBufferOfHeader, byteBufferOfBody };
        long numberOfBytesRead = acceptedSocketChannel.read(byteBuffers);
        if (numberOfBytesRead < 0) {
            return;
        }
        LOG.info("[Data] | Server reads bytes from client {} | bytes: {}", acceptedSocketChannel.getRemoteAddress(), numberOfBytesRead);

        // Read the data from the byte buffers
        Packet packet = Packet.readOnServer(byteBufferOfHeader, byteBufferOfBody);
        LOG.info("[Data] | Server reads packet from client {} | packet: {}", acceptedSocketChannel.getRemoteAddress(), packet);

        // Process received packet and return a new one
        Packet packetToSend = processReceivedPacket(packet);

        // Register the accepted socket channel to selector
        SelectionKey registeredKey = acceptedSocketChannel.register(selector, SelectionKey.OP_WRITE, packetToSend);
        logSelectionKeyInfo(selectedKey, "read", registeredKey, "write");
    }

    @Override
    public void processWriteEvent(SelectionKey selectedKey) throws Exception {
        // Get the accepted socket channel
        SocketChannel acceptedSocketChannel = (SocketChannel) selectedKey.channel();
        if (acceptedSocketChannel == null) {
            return;
        }

        // Get the packet to send
        Packet packetToSend = (Packet) selectedKey.attachment();

        // Get the byte buffers from packet
        ByteBuffer[] byteBuffers = packetToSend.createByteBuffersOnServer(ByteBufferType.DIRECT);
        // Gather-write the byte buffers to client
        long numberOfBytesWritten = acceptedSocketChannel.write(byteBuffers);
        if (numberOfBytesWritten < 0) {
            return;
        }
        LOG.info("[Data] | Server writes bytes to client {} | bytes: {}", acceptedSocketChannel.getRemoteAddress(), numberOfBytesWritten);
        LOG.info("[Data] | Server writes packet to client {} | packet: {}", acceptedSocketChannel.getRemoteAddress(), packetToSend);

        // Register the accepted socket channel to selector
        SelectionKey registeredKey = acceptedSocketChannel.register(selector, SelectionKey.OP_READ);
        logSelectionKeyInfo(selectedKey, "write", registeredKey, "read");

        // Close the accepted socket channel
//        selectedKey.cancel();
//        closeAcceptedSocketChannel(acceptedSocketChannel);
    }

    @Override
    public synchronized Packet processReceivedPacket(Packet receivedPacket) {
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
                Long snowflakeId = null;
                SnowFlakeIdWorker snowFlakeIdWorker = SnowFlakeIdWorkerFactory.getWorker(snowflakeIdRequestBody.getAppCode());
                if (snowFlakeIdWorker == null) {
                    LOG.error("Snowflake ID worker is null");
                } else {
                    snowflakeId = snowFlakeIdWorker.getNextId();
                }
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

    /********************************* Setup and Shutdown *********************************/

    /**
     * This method is used to close the accepted socket <br/>
     *
     * @param acceptedSocketChannel
     */
    private void closeAcceptedSocketChannel(SocketChannel acceptedSocketChannel) {
        try {
            if (acceptedSocketChannel.socket().isClosed()) {
                return;
            }
            selector.wakeup();
            acceptedSocketChannel.socket().shutdownInput();
            acceptedSocketChannel.socket().shutdownOutput();
            acceptedSocketChannel.socket().close();
            acceptedSocketChannel.close();
            LOG.info("Accepted socket is closed");
        } catch (Exception e) {
            LOG.error("Close client connection error", e);
        }
    }

    /************************************** State Machine **************************************/

    public void setServerSocketState(ServerSocketState serverSocketState) {
        this.serverSocketState = serverSocketState;
        LOG.info("Server socket state is set to {}", serverSocketState);
    }

    public ServerSocketState getServerSocketState() {
        return serverSocketState;
    }
}
