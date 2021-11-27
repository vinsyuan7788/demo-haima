package com.demo.haima.fundamental.server.simplex.nonblocking.socket;

import com.demo.haima.fundamental.server.simplex.nonblocking.SerdesSimplexServer;
import com.demo.haima.fundamental.utils.auxiliary.ContainerRunner;
import com.demo.haima.fundamental.utils.data.network.packet.Packet;
import com.demo.haima.fundamental.utils.state.server.nonblocking.ServerSocketState;
import com.demo.haima.fundamental.utils.state.server.nonblocking.ServerState;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.CancelledKeyException;
import java.nio.channels.ClosedChannelException;
import java.nio.channels.ClosedSelectorException;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;

/**
 * @author Vince Yuan
 * @date 2021/11/20
 */
public class NioSerdesSimplexServerSocket extends ContainerRunner implements Runnable, SerdesSimplexServerSocket {

    private static final Logger LOG = LoggerFactory.getLogger(NioSerdesSimplexServerSocket.class);

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

    private SerdesSimplexServer server;
    private ContainerRunner containerRunner;
    public ServerSocketChannel serverSocketChannel;
    private Selector selector;

    private ServerSocketState serverSocketState;

    public NioSerdesSimplexServerSocket(SerdesSimplexServer server, int portToBind) {
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
            LOG.info("Server socket channel is registered to selector with interest of accept event |  | registered key: {}", registeredKey);
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
        List<SelectionKey> selectedKeyList = new ArrayList<>(selectedKeys);
        Collections.shuffle(selectedKeyList);
        for (SelectionKey selectedKey : selectedKeyList) {
            if (selectedKey.isAcceptable()) {
                processAcceptEvent(selectedKey);
            } else if (selectedKey.isConnectable()) {
                // Do nothing here
            } else if (selectedKey.isReadable()) {
                processReadEvent(selectedKey);
            } else if (selectedKey.isWritable()) {
                // Do nothing here
            }
        }
        selectedKeyList.clear();
    }

    @Override
    public void handleRunningThrowable(Throwable t) {
        if (t instanceof ClosedSelectorException) {
            LOG.error("Please ensure current thread stops running before closing the selector", t);
        } else if (t instanceof CancelledKeyException) {
            LOG.error("Raised during the iteration of process selected key. If the selected key is " +
                    "canceled and not removed in time after each iteration, it will be raised, which " +
                    "however, does not affect the server processing result", t);
        } else if (t instanceof ClosedChannelException) {
            // This exception can be ignored since it is raised by "closeSocket()" during the execution of
            // server socket channel's accept. Since we want to close the server, the server socket channel
            // needs to be interrupted for sure. Hence, this exception is fine here in this scenario
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
    public void processReadEvent(SelectionKey selectedKey) throws Exception {
        // Get the accepted socket channel
        SocketChannel acceptedSocketChannel = (SocketChannel) selectedKey.channel();
        if (acceptedSocketChannel == null) {
            return;
        }

        // Read the data sent from client through the channel into a byte buffer
        ByteBuffer byteBuffer = ByteBuffer.allocateDirect(1024);
        int numberOfBytesRead = acceptedSocketChannel.read(byteBuffer);
        if (numberOfBytesRead < 0) {
            return;
        }
        LOG.info("[Data] | Server reads bytes from client {} | bytes: {}", acceptedSocketChannel.getRemoteAddress(), numberOfBytesRead);

        // Read the data from the byte buffer
        Packet packet = Packet.readOnServer(byteBuffer);
        LOG.info("[Data] | Server reads packet from client {} | packet: {}", acceptedSocketChannel.getRemoteAddress(), packet);

        // Register the accepted socket channel to selector
        SelectionKey registeredKey = acceptedSocketChannel.register(selector, SelectionKey.OP_READ);
        logSelectionKeyInfo(selectedKey, "read", registeredKey, "read");

        // Close the accepted socket channel
        selectedKey.cancel();
        closeAcceptedSocketChannel(acceptedSocketChannel);
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
