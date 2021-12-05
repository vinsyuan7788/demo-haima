package com.demo.haima.fundamental.server.simplex.asynchronous.socket;

import com.demo.haima.fundamental.server.simplex.asynchronous.socket.handler.AioBaseSimplexServerSocketAcceptCompletionHandler;
import com.demo.haima.fundamental.server.simplex.asynchronous.socket.handler.AioBaseSimplexServerSocketReadCompletionHandler;
import com.demo.haima.fundamental.utility.auxiliary.AioImplementationWay;
import com.demo.haima.fundamental.utility.auxiliary.ContainerRunner;
import com.demo.haima.fundamental.utility.exception.DemoException;
import com.demo.haima.fundamental.utility.state.server.asynchronous.ServerSocketState;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.InetSocketAddress;
import java.net.StandardSocketOptions;
import java.nio.ByteBuffer;
import java.nio.channels.AcceptPendingException;
import java.nio.channels.AsynchronousChannelGroup;
import java.nio.channels.AsynchronousServerSocketChannel;
import java.nio.channels.AsynchronousSocketChannel;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * @author Vince Yuan
 * @date 2021/11/20
 */
public class AioBaseSimplexServerSocket extends ContainerRunner implements Runnable, BaseSimplexServerSocket {

    private static final Logger LOG = LoggerFactory.getLogger(AioBaseSimplexServerSocket.class);

    private AioImplementationWay runWhichImplementation;

    private ContainerRunner containerRunner;

    private AsynchronousChannelGroup channelGroup;
    public AsynchronousServerSocketChannel serverSocketChannel;
    private AioBaseSimplexServerSocketAcceptCompletionHandler acceptCompletionHandler;

    private volatile ServerSocketState serverSocketState;

    /**
     * This field represents the accept event to be completed, normally its value is 1,
     * which signifies that the server socket is waiting for one connection from request.
     * When server socket channel starts an accept event (by "accept" method), it will increment.
     * When accept event completion handler is invoked and executed normally, it will decrement.
     */
    private AtomicInteger acceptingTracker;
    /**
     * This field represents when the received data starts or ends processing.
     * When receiving data, it will increment.
     * When completion handler completes processing received data, it will decrement.
     */
    private AtomicInteger processingTracker;

    public AioBaseSimplexServerSocket(AioImplementationWay runWhichImplementation, int portToBind) {
        try {
            this.runWhichImplementation = runWhichImplementation;
            containerRunner = new ContainerRunner(this, "ServerSocket");
            if (runWhichImplementation == AioImplementationWay.COMPLETION_HANDLER) {
                acceptCompletionHandler = new AioBaseSimplexServerSocketAcceptCompletionHandler(new AioBaseSimplexServerSocketReadCompletionHandler());
                acceptingTracker = new AtomicInteger(0);
                processingTracker = new AtomicInteger(0);
            }

            setServerSocketState(ServerSocketState.INITIAL);
            ExecutorService defaultCachedThreadPool = getDefaultCachedThreadPool("AIO-ServerSocket-EventHandler");
            channelGroup = AsynchronousChannelGroup.withThreadPool(defaultCachedThreadPool);
            serverSocketChannel = AsynchronousServerSocketChannel.open(channelGroup);
            serverSocketChannel.bind(new InetSocketAddress(portToBind));
            setServerSocketState(ServerSocketState.BOUND);
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
        if (t instanceof ExecutionException) {
            // This exception can be ignored if raised by get from future of accept,
            // since it is caused by 'closeSocket' in-between two 'serverSocketChannel.isOpen()' in while-loop in 'runWithFuture()'
            // when server socket is in the execution of 'get'. It does not affect server processing received data
        } else if (t instanceof AcceptPendingException) {
            LOG.info("Process socket error", t);
        } else {
            LOG.info("Process socket error", t);
        }
    }

    /********************************* Data Processing *********************************/

    public void runWithFuture() {
        while (serverSocketChannel.isOpen()) {
            try {
                // Server socket channel starts an accept event to be completed
                Future<AsynchronousSocketChannel> futureOfAcceptCompletionResult = serverSocketChannel.accept();
                if (futureOfAcceptCompletionResult == null) {
                    continue;
                }

                // Accept the socket channel from client once the event is completed
                AsynchronousSocketChannel acceptedSocketChannel = futureOfAcceptCompletionResult.get();
                if (acceptedSocketChannel == null) {
                    continue;
                }

                // Configure the accepted socket
                acceptedSocketChannel.setOption(StandardSocketOptions.TCP_NODELAY, true);
                // set socket linger to false, so that socket close does not block
                // -- this option is not supported here in AIO
//                acceptedSocketChannel.setOption(StandardSocketOptions.SO_LINGER, -1);

                // Start a read event to be completed that reads the data
                // sent from client through the channel into a byte buffer
                ByteBuffer byteBuffer = ByteBuffer.allocateDirect(1024);
                Future<Integer> futureOfReadCompletionResult = acceptedSocketChannel.read(byteBuffer);

                // Get the number of bytes read once the event is completed
                Integer numberOfBytesRead = futureOfReadCompletionResult.get();
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

                // Close the accepted socket channel
                closeAcceptedSocketChannel(acceptedSocketChannel);
            } catch (Throwable t) {
                handleRunningThrowable(t);
            }
        }
    }

    private void runWithCompletionHandler() {
        while (serverSocketChannel.isOpen()) {
            try {
                // If there is one accept event to be completed.
                // In this case there is no need to execute "accept()" again, otherwise AcceptPendingException will be raised
                if (acceptingTracker.get() > 0) { continue; }
                // Start an accept event that will be completed any time if there is a connection coming from client.
                // Once the event is completed, the corresponding completion handler will be invoked
                serverSocketChannel.accept(AioBaseSimplexServerSocketAcceptCompletionHandler.Attachment.create(this), acceptCompletionHandler);
                // Update the number of accept event to be completed
                acceptingTracker.incrementAndGet();
            } catch (Throwable t) {
                handleRunningThrowable(t);
            }
        }
    }

    /********************************* Setup and Shutdown *********************************/

    private void closeSocketWithFuture() {
        try {
            if (!serverSocketChannel.isOpen()) {
                return;
            }
            serverSocketChannel.close();
            channelGroup.shutdown();
            // Wait until server socket thread stops running
            containerRunner.join();
            setServerSocketState(ServerSocketState.CLOSED);
        } catch (Throwable t) {
            handleClosingThrowable(t);
        }
    }

    private void closeSocketWithCompletionHandler() {
        try {
            while (acceptingTracker.get() < 1 || processingTracker.get() > 0) { }
            if (!serverSocketChannel.isOpen()) {
                return;
            }
            serverSocketChannel.close();
            channelGroup.shutdown();
            // Wait until server socket thread stops running
            containerRunner.join();
            setServerSocketState(ServerSocketState.CLOSED);
        } catch (Throwable t) {
            handleClosingThrowable(t);
        }
    }

    /**
     * This method is used to close the accepted socket <br/>
     *
     * @param acceptedSocketChannel
     */
    public void closeAcceptedSocketChannel(AsynchronousSocketChannel acceptedSocketChannel) {
        try {
            if (!acceptedSocketChannel.isOpen()) {
                return;
            }
            acceptedSocketChannel.close();
            LOG.info("Accepted socket is closed");
        } catch (Exception e) {
            LOG.error("Close client connection error", e);
        }
    }

    /**************************************** Getter and Setter ****************************************/

    public AsynchronousServerSocketChannel getServerSocketChannel() {
        return serverSocketChannel;
    }

    public AtomicInteger getAcceptingTracker() {
        return acceptingTracker;
    }

    public AtomicInteger getProcessingTracker() {
        return processingTracker;
    }

    /**************************************** State Machine ****************************************/

    public void setServerSocketState(ServerSocketState serverSocketState) {
        this.serverSocketState = serverSocketState;
        LOG.info("Server socket state is set to {}", serverSocketState);
    }

    public ServerSocketState getServerSocketState() {
        return serverSocketState;
    }

    /**************************************** Utility Method ****************************************/

    /**
     * This method is used to update necessary trackers
     */
    public synchronized void updateTracker() {
        acceptingTracker.decrementAndGet();
        processingTracker.incrementAndGet();
    }
}
