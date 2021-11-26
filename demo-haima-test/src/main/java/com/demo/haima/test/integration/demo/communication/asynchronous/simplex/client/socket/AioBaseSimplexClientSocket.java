package com.demo.haima.test.integration.demo.communication.asynchronous.simplex.client.socket;

import com.demo.haima.test.integration.demo.communication.asynchronous.simplex.client.socket.handler.AioBaseSimpleClientSocketWriteCompletionHandler;
import com.demo.haima.test.integration.demo.communication.asynchronous.simplex.client.socket.handler.AioBaseSimplexClientSocketConnectCompletionHandler;
import com.demo.haima.test.integration.demo.utils.auxiliary.AioImplementationWay;
import com.demo.haima.test.integration.demo.utils.auxiliary.ContainerRunner;
import com.demo.haima.test.integration.demo.utils.exception.DemoException;
import com.demo.haima.test.integration.demo.utils.state.client.aio.ClientSocketState;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.SocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.AsynchronousChannelGroup;
import java.nio.channels.AsynchronousSocketChannel;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * @author Vince Yuan
 * @date 2021/11/20
 */
public class AioBaseSimplexClientSocket extends ContainerRunner implements Runnable, BaseSimplexClientSocket {

    private static final Logger LOG = LoggerFactory.getLogger(AioBaseSimplexClientSocket.class);

    private AioImplementationWay runWhichImplementation;

    private ContainerRunner containerRunner;
    private SocketAddress serverAddressToConnect;

    private AsynchronousChannelGroup channelGroup;
    private AsynchronousSocketChannel clientSocketChannel;
    private AioBaseSimplexClientSocketConnectCompletionHandler connectCompletionHandler;

    private ClientSocketState clientSocketState;

    /**
     * This field represents the connect event to be completed. Its initial value is 1,
     * which signifies that the client socket is waiting for one reception from server.
     * When connect event completion handler is invoked and executed normally, it will decrement.
     */
    private AtomicInteger connectingTracker;
    /**
     * This field represents when the data to send starts or ends processing. Its initial value is 1,
     * which signifies that the client only needs to send data once.
     * When completion handler completes sending data, it will decrement.
     */
    private AtomicInteger processingTracker;

    public AioBaseSimplexClientSocket(AioImplementationWay runWhichImplementation, SocketAddress serverAddressToConnect) {
        try {
            this.runWhichImplementation = runWhichImplementation;
            containerRunner = new ContainerRunner(this, "ClientSocket");
            this.serverAddressToConnect = serverAddressToConnect;
            if (runWhichImplementation == AioImplementationWay.COMPLETION_HANDLER) {
                connectCompletionHandler = new AioBaseSimplexClientSocketConnectCompletionHandler(new AioBaseSimpleClientSocketWriteCompletionHandler());
                connectingTracker = new AtomicInteger(1);
                processingTracker = new AtomicInteger(1);
            }

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
        switch (runWhichImplementation) {
            case FUTURE:
                runWithFuture();
                break;
            case COMPLETION_HANDLER:
                runWithCompletionHandler();
                break;
            default:
                throw new DemoException("AIO implementation is unknown");
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
        switch (runWhichImplementation) {
            case FUTURE:
                closeSocketWithFuture();
                break;
            case COMPLETION_HANDLER:
                closeSocketWithCompletionHandler();
                break;
            default:
                throw new DemoException("AIO implementation is unknown");
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
    public void handleRunningThrowable(Throwable t) {
        LOG.info("Process socket error", t);
    }

    /**************************************** Data Processing ****************************************/

    private void runWithFuture() {
        try {
            // Client socket channel starts a connect event to be completed
            Future<Void> futureOfConnectCompletionResult = clientSocketChannel.connect(serverAddressToConnect);
            setClientSocketState(ClientSocketState.CONNECTING);

            // Wait until the client socket is connected to server (i.e., the event is completed)
            futureOfConnectCompletionResult.get();
            LOG.info("Client socket is connected to {}", serverAddressToConnect);
            setClientSocketState(ClientSocketState.CONNECTED);
            logSocketChannelInfo(clientSocketChannel);

            // Wrap the data into a byte buffer
            // NOTICE: the wrap method will set buffer's position to zero, hence no need to flip it
            String data = "Hello NIO";
            ByteBuffer byteBuffer = ByteBuffer.wrap(data.getBytes());
            // Send the byte buffer to server
            Future<Integer> futureOfWriteResult = clientSocketChannel.write(byteBuffer);
            Integer numberOfBytesWritten = futureOfWriteResult.get();
            LOG.info("[Data] | Client writes data to server {} | data: {} | number of bytes written: {}", serverAddressToConnect, data, numberOfBytesWritten);
        } catch (Throwable t) {
            handleRunningThrowable(t);
        }
    }

    private void runWithCompletionHandler() {
        try {
            clientSocketChannel.connect(serverAddressToConnect, AioBaseSimplexClientSocketConnectCompletionHandler.Attachment.create(this), connectCompletionHandler);
            setClientSocketState(ClientSocketState.CONNECTING);
        } catch (Throwable t) {
            handleRunningThrowable(t);
        }
    }

    /**************************************** Setup and Shutdown ****************************************/

    private void closeSocketWithFuture() {
        try {
            // To ensure the communication with server can be done
            // before closing resources (e.g., socket channel, socket)
            containerRunner.join();
            if (!clientSocketChannel.isOpen()) {
                return;
            }
            clientSocketChannel.close();
            setClientSocketState(ClientSocketState.CLOSED);
            LOG.info("Client socket channel is closed");
        } catch (Throwable t) {
            handleClosingThrowable(t);
        }
    }

    private void closeSocketWithCompletionHandler() {
        try {
            while (connectingTracker.get() > 0 || processingTracker.get() > 0) { }
            if (!clientSocketChannel.isOpen()) {
                return;
            }
            clientSocketChannel.close();
            containerRunner.join();
            setClientSocketState(ClientSocketState.CLOSED);
        } catch (Throwable t) {
            handleClosingThrowable(t);
        }
    }

    /**************************************** Getter and Setter ****************************************/

    public SocketAddress getServerAddressToConnect() {
        return serverAddressToConnect;
    }

    public AsynchronousSocketChannel getClientSocketChannel() {
        return clientSocketChannel;
    }

    public AtomicInteger getProcessingTracker() {
        return processingTracker;
    }

    public AtomicInteger getConnectingTracker() {
        return connectingTracker;
    }

    /**************************************** State Machine ****************************************/

    public synchronized void setClientSocketState(ClientSocketState clientSocketState) {
        this.clientSocketState = clientSocketState;
        LOG.info("Client socket state is set to {}", clientSocketState);
    }

    public ClientSocketState getClientSocketState() {
        return clientSocketState;
    }
}
