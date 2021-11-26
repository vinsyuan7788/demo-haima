package com.demo.haima.test.integration.demo.utils.auxiliary;

import com.demo.haima.common.utility.LogUtils;
import com.demo.haima.test.integration.demo.utils.provider.AsynchronousChannelInfoProvider;
import com.demo.haima.test.integration.demo.utils.provider.SelectableChannelBlockingModeProvider;
import com.demo.haima.test.integration.demo.utils.provider.SelectionKeyInfoProvider;
import com.demo.haima.test.integration.demo.utils.provider.SocketChannelInfoProvider;
import com.demo.haima.test.integration.demo.utils.provider.ThreadPoolProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.InetAddress;
import java.net.SocketAddress;
import java.nio.channels.AsynchronousServerSocketChannel;
import java.nio.channels.AsynchronousSocketChannel;
import java.nio.channels.SelectableChannel;
import java.nio.channels.SelectionKey;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.RejectedExecutionHandler;
import java.util.concurrent.SynchronousQueue;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * This class is used to run a container (i.e., server or client)
 *
 * @author Vince Yuan
 * @date 2021/11/20
 */
public class ContainerRunner extends Thread implements SelectableChannelBlockingModeProvider, SocketChannelInfoProvider, SelectionKeyInfoProvider, AsynchronousChannelInfoProvider, ThreadPoolProvider {

    private static final Logger LOG = LoggerFactory.getLogger(ContainerRunner.class);

    private String className = getClass().getSimpleName();

    public ContainerRunner() {
        super();
    }

    public ContainerRunner(Runnable threadToRun, String threadName) {
        super(threadToRun, threadName);
    }

    @Override
    public String getSelectableChannelBlockingMode(SelectableChannel selectableChannel) {
        return selectableChannel.isBlocking() ? "blocking mode" : "non-blocking mode";
    }

    @Override
    public void logSocketChannelInfo(SocketChannel socketChannel) throws Exception {
        SocketAddress localAddress = socketChannel.getLocalAddress();
        SocketAddress localSocketAddress = socketChannel.socket().getLocalSocketAddress();
        InetAddress localIpAddress = socketChannel.socket().getLocalAddress();
        SocketAddress remoteAddress = socketChannel.getRemoteAddress();
        SocketAddress remoteSocketAddress = socketChannel.socket().getRemoteSocketAddress();
        InetAddress remoteIpAddress = socketChannel.socket().getInetAddress();
        LOG.info(LogUtils.getMessage(className + "#logSocketChannelInfo", "local address of socket channel: {}, {}, {}"
                + " | remote address of socket channel: {}, {}, {}"), localAddress, localSocketAddress, localIpAddress, remoteAddress, remoteSocketAddress, remoteIpAddress);
    }

    @Override
    public void logServerSocketChannelInfo(ServerSocketChannel serverSocketChannel) throws Exception {
        SocketAddress localAddress = serverSocketChannel.getLocalAddress();
        SocketAddress localSocketAddress = serverSocketChannel.socket().getLocalSocketAddress();
        InetAddress remoteIpAddress = serverSocketChannel.socket().getInetAddress();
        LOG.info(LogUtils.getMessage(className + "#logServerSocketChannelInfo", "local address of server socket channel: {}, {}"
                + " | remote address of server socket channel: {}"), localAddress, localSocketAddress, remoteIpAddress);
    }

    @Override
    public void logSelectionKeyInfo(SelectionKey selectedKey, String selectedEvents, SelectionKey registeredKey, String registeredEvents) {
        LOG.info(LogUtils.getMessage(className + "#logSelectionKeyInfo", "selected key: {} | selected events: {} | registered key: {} | registered events: {}"),
                selectedKey, selectedEvents, registeredKey, registeredEvents);
    }

    @Override
    public void logSocketChannelInfo(AsynchronousSocketChannel socketChannel) throws Exception {
        SocketAddress localSocketAddress = socketChannel.getLocalAddress();
        SocketAddress remoteSocketAddress = socketChannel.getRemoteAddress();
        LOG.info(LogUtils.getMessage(className + "#logSocketChannelInfo", "local address of socket channel: {}"
                + " | remote address of socket channel: {}"), localSocketAddress, remoteSocketAddress);
    }

    @Override
    public void logServerSocketChannelInfo(AsynchronousServerSocketChannel serverSocketChannel) throws Exception {
        SocketAddress localSocketAddress = serverSocketChannel.getLocalAddress();
        LOG.info(LogUtils.getMessage(className + "#logServerSocketChannelInfo", "local address of server socket channel: {}"), localSocketAddress);
    }

    @Override
    public ExecutorService getDefaultCachedThreadPool(String threadName) {
        return new ThreadPoolExecutor(0, Integer.MAX_VALUE, 60L, TimeUnit.SECONDS, new SynchronousQueue<>(), new DefaultThreadFactory(threadName), new AbortPolicy());
    }

    /**
     * This class is a default thread factory to provide a thread.
     * Here is the same implementation from {@link ThreadPoolExecutor}
     */
    static class DefaultThreadFactory implements ThreadFactory {
        private static final AtomicInteger poolNumber = new AtomicInteger(1);
        private final ThreadGroup group;
        private final AtomicInteger threadNumber = new AtomicInteger(1);
        private final String namePrefix;

        DefaultThreadFactory(String threadName) {
            SecurityManager s = System.getSecurityManager();
            group = (s != null) ? s.getThreadGroup() : Thread.currentThread().getThreadGroup();
            namePrefix = "pool-" + poolNumber.getAndIncrement() + "-" + threadName + "-thread-";
        }

        @Override
        public Thread newThread(Runnable r) {
            Thread t = new Thread(group, r, namePrefix + threadNumber.getAndIncrement(), 0);
            if (t.isDaemon()) {
                t.setDaemon(false);
            }
            if (t.getPriority() != Thread.NORM_PRIORITY) {
                t.setPriority(Thread.NORM_PRIORITY);
            }
            return t;
        }
    }

    /**
     * This class is default-implemented rejected execution handler.
     * Here is the same implementation from {@link ThreadPoolExecutor}
     */
    static class AbortPolicy implements RejectedExecutionHandler {

        /**
         * Creates an {@code AbortPolicy}.
         */
        public AbortPolicy() { }

        /**
         * Always throws RejectedExecutionException.
         *
         * @param r the runnable task requested to be executed
         * @param e the executor attempting to execute this task
         * @throws RejectedExecutionException always
         */
        @Override
        public void rejectedExecution(Runnable r, ThreadPoolExecutor e) {
            throw new RejectedExecutionException("Task " + r.toString() + " rejected from " + e.toString());
        }
    }
}
