package com.demo.haima.fundamental.server.duplex.asynchronous.socket;

import com.demo.haima.fundamental.server.duplex.asynchronous.DemoDuplexServer;
import com.demo.haima.fundamental.server.duplex.asynchronous.socket.handler.AioDemoDuplexServerSocketAcceptCompletionHandler;
import com.demo.haima.fundamental.server.duplex.asynchronous.socket.handler.AioDemoDuplexServerSocketReadCompletionHandler;
import com.demo.haima.fundamental.server.duplex.asynchronous.socket.handler.AioDemoDuplexServerSocketWriteCompletionHandler;
import com.demo.haima.fundamental.utility.auxiliary.ContainerRunner;
import com.demo.haima.fundamental.utility.business.snowflake.factory.SnowFlakeIdWorkerFactory;
import com.demo.haima.fundamental.utility.state.server.asynchronous.ServerSocketState;
import com.demo.haima.fundamental.utility.state.server.asynchronous.ServerState;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.InetSocketAddress;
import java.nio.channels.AsynchronousChannelGroup;
import java.nio.channels.AsynchronousServerSocketChannel;
import java.nio.channels.AsynchronousSocketChannel;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * @author Vince Yuan
 * @date 2021/11/24
 */
public class AioDemoDuplexServerSocket extends ContainerRunner implements Runnable, DemoDuplexServerSocket {

    private static final Logger LOG = LoggerFactory.getLogger(AioDemoDuplexServerSocket.class);

    private DemoDuplexServer server;
    private ContainerRunner containerRunner;
    private AsynchronousChannelGroup channelGroup;
    private AsynchronousServerSocketChannel serverSocketChannel;
    private AioDemoDuplexServerSocketAcceptCompletionHandler acceptCompletionHandler;

    private ServerSocketState serverSocketState;
    
    private AtomicInteger acceptingTracker;

    public AioDemoDuplexServerSocket(DemoDuplexServer server, int portToBind) {
        try {
            this.server = server;
            this.server.setServerState(ServerState.INITIAL);
            containerRunner = new ContainerRunner(this, "ServerSocket");
            acceptCompletionHandler = buildAcceptCompletionHandler();
            acceptingTracker = new AtomicInteger(0);

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
        server.setServerState(ServerState.RUNNING);
        while (serverSocketChannel.isOpen()) {
            try {
                if (acceptingTracker.get() > 0) { continue; }
                serverSocketChannel.accept(AioDemoDuplexServerSocketAcceptCompletionHandler.Attachment.create(this), acceptCompletionHandler);
                acceptingTracker.incrementAndGet();
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
            if (!serverSocketChannel.isOpen()) {
                return;
            }
            serverSocketChannel.close();
            channelGroup.shutdown();
            // Wait until server socket thread stops running
            containerRunner.join();
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
    public void handleRunningThrowable(Throwable t) {
        LOG.error("Process socket error", t);
    }

    /************************************* Setup and Shutdown *************************************/

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

    /************************************* Setup and Shutdown *************************************/

    public void setServerSocketState(ServerSocketState serverSocketState) {
        this.serverSocketState = serverSocketState;
        LOG.info("Server socket state is set to {}", serverSocketState);
    }

    public ServerSocketState getServerSocketState() {
        return serverSocketState;
    }

    /************************************* Getter and Setter *************************************/

    public AsynchronousServerSocketChannel getServerSocketChannel() {
        return serverSocketChannel;
    }

    public AtomicInteger getAcceptingTracker() {
        return acceptingTracker;
    }

    /************************************* Utility Method *************************************/

    private AioDemoDuplexServerSocketAcceptCompletionHandler buildAcceptCompletionHandler() {
        AioDemoDuplexServerSocketReadCompletionHandler readCompletionHandler = new AioDemoDuplexServerSocketReadCompletionHandler();
        AioDemoDuplexServerSocketWriteCompletionHandler writeCompletionHandler = new AioDemoDuplexServerSocketWriteCompletionHandler();
        readCompletionHandler.setWriteCompletionHandler(writeCompletionHandler);
        writeCompletionHandler.setReadCompletionHandler(readCompletionHandler);
        return new AioDemoDuplexServerSocketAcceptCompletionHandler(readCompletionHandler);
    }
}
