package com.demo.haima.fundamental.server.simplex.nonblocking.socket;

import com.demo.haima.fundamental.utility.auxiliary.ContainerRunner;
import com.demo.haima.fundamental.utility.state.server.nonblocking.ServerSocketState;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.ClosedSelectorException;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.util.Iterator;
import java.util.Set;

/**
 * @author Vince Yuan
 * @date 2021/11/20
 */
public class NioBaseSimplexServerSocket extends ContainerRunner implements Runnable, BaseSimplexServerSocket {

    private static final Logger LOG = LoggerFactory.getLogger(NioBaseSimplexServerSocket.class);

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

    private ContainerRunner containerRunner;
    public ServerSocketChannel serverSocketChannel;
    private Selector selector;

    private ServerSocketState serverSocketState;

    public NioBaseSimplexServerSocket(int portToBind) {
        try {
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
        Iterator<SelectionKey> selectedKeyIterator = selectedKeys.iterator();
        while (selectedKeyIterator.hasNext()) {
            SelectionKey selectedKey = selectedKeyIterator.next();
            if (selectedKey.isAcceptable()) {
                // Accept the socket channel from client
                ServerSocketChannel serverSocketChannel = (ServerSocketChannel) selectedKey.channel();
                if (serverSocketChannel == null) {
                    continue;
                }
                logServerSocketChannelInfo(serverSocketChannel);
                SocketChannel acceptedSocketChannel = serverSocketChannel.accept();
                if (acceptedSocketChannel == null) {
                    continue;
                }
                logSocketChannelInfo(acceptedSocketChannel);
                // Configure the accepted socket
                acceptedSocketChannel.socket().setTcpNoDelay(true);
                // set socket linger to false, so that socket close does not block
                acceptedSocketChannel.socket().setSoLinger(false, -1);
                // Register the accepted socket channel to selector
                acceptedSocketChannel.configureBlocking(false);
                int selectedOpCode = selectedKey.interestOps();
                SelectionKey registeredKey = acceptedSocketChannel.register(selector, SelectionKey.OP_READ);
                int registeredOpCode = registeredKey.interestOps();
                logSelectionKeyInfo(selectedKey, selectedOpCode, registeredKey, registeredOpCode);
            } else if (selectedKey.isConnectable()) {
                // Do nothing here
            } else if (selectedKey.isReadable()) {
                // Get the accepted socket channel
                SocketChannel acceptedSocketChannel = (SocketChannel) selectedKey.channel();
                if (acceptedSocketChannel == null) {
                    continue;
                }
                // Read the data sent from client through the channel into a byte buffer
                ByteBuffer byteBuffer = ByteBuffer.allocateDirect(1024);
                int numberOfBytesRead = acceptedSocketChannel.read(byteBuffer);
                if (numberOfBytesRead <= 0) {
                    continue;
                }
                LOG.info("[Data] | Server reads bytes from client {} | bytes: {}", acceptedSocketChannel.getRemoteAddress(), numberOfBytesRead);
                // Flip the byte buffer so that the data within it can be read to target byte array
                byteBuffer.flip();
                // Read the data in the buffer to target byte array
                byte[] dataBytesRead = new byte[numberOfBytesRead];
                byteBuffer.get(dataBytesRead);
                String data = new String(dataBytesRead);
                LOG.info("[Data] | Server reads data from client {} | data: {}", acceptedSocketChannel.getRemoteAddress(), data);
                // Register the accepted socket channel to selector
                int selectedOpCode = selectedKey.interestOps();
                SelectionKey registeredKey = acceptedSocketChannel.register(selector, SelectionKey.OP_READ);
                int registeredOpCode = registeredKey.interestOps();
                logSelectionKeyInfo(selectedKey, selectedOpCode, registeredKey, registeredOpCode);
                // Close the accepted socket channel
                closeAcceptedSocketChannel(acceptedSocketChannel);
            } else if (selectedKey.isWritable()) {
                // Do nothing here
            }
            selectedKeyIterator.remove();
        }
    }

    @Override
    public void handleRunningThrowable(Throwable t) {
        if (t instanceof ClosedSelectorException) {
            LOG.error("Please ensure current thread stops running before closing the selector", t);
        } else {
            LOG.error("Process socket error", t);
        }
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
