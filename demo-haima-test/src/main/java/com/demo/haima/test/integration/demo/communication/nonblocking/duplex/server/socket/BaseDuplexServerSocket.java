package com.demo.haima.test.integration.demo.communication.nonblocking.duplex.server.socket;

import java.nio.channels.ClosedSelectorException;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.util.Set;

/**
 * @author Vince Yuan
 * @date 2021/11/20
 */
public interface BaseDuplexServerSocket {

    /************************ Setup and Shutdown ************************/

    /**
     * This method is used to start the socket
     */
    void startSocket();

    /**
     * This method is used to close the socket.
     * In this method, close the selector after ensuring that the server socket
     * thread stops running, otherwise it will cause {@link ClosedSelectorException}.
     * See {@link #handleRunningThrowable(Throwable)} for more details
     */
    void closeSocket();

    /**
     * This method is used to handle throwable during initializing
     *
     * @param t
     */
    void handleInitializingThrowable(Throwable t);

    /**
     * This method is used to handle throwable during starting
     *
     * @param t
     */
    void handleStartingThrowable(Throwable t);

    /**
     * This method is used to handle throwable during closing
     *
     * @param t
     */
    void handleClosingThrowable(Throwable t);

    /************************ Data Processing ************************/

    /**
     * This method is used to select keys whose channels are ready for registered events.
     * In this method, the return value of {@link Selector#select(long)} is NOT reliable
     * specifically for ready channels for registered events, if there are other method
     * invocation of {@link Selector} (e.g., {@link Selector#wakeup()}) or other situation
     * (e.g., current thread is interrupted) involved.
     * Hence, here the return value is NOT our concern, and we leave ready channels to be
     * returned by {@link #getSelectedKeys(Selector)}.
     *
     * @param selector
     * @return
     * @throws Exception
     */
    void selectKeys(Selector selector) throws Exception;

    /**
     * This method is used to get selected keys whose channels are ready for
     * registered events.
     * If {@link #selectKeys(Selector)} is returned due to other method invocation
     * of {@link Selector} (e.g., {@link Selector#wakeup()}) or other situation
     * (e.g., current thread is interrupted). This method will return an empty set
     * (i.e., whose size is 0) that signifies that there is actually no ready channel.
     * Notice that the return set is the set maintained by the selector, so no matter
     * how many times this method is invoked from the same selector, it will return
     * the same set, which is why it is better (but not required) to clear this set
     * after we process this set (see {@link #processSelectedKeys(Set)})
     *
     * @param selector
     * @return
     * @throws Exception
     */
    Set<SelectionKey> getSelectedKeys(Selector selector);

    /**
     * This method is used to process selected keys whose channels are ready for
     * registered events.
     * After we clean this set, it is better (but not required) to clean it to
     * ensure next time we process this set, there will be no ready channels that
     * have been processed.
     * For more information, see {@link #getSelectedKeys(Selector)}
     *
     * @param selectedKeys
     * @throws Exception
     */
    void processSelectedKeys(Set<SelectionKey> selectedKeys) throws Exception;

    /**
     * This method is used to process throwable during socket running. <br/>
     * For {@link ClosedSelectorException}: this exception occurs when selector is
     * closed while server socket is yet closed (since closing socket is OS-level
     * operation, and closing selector is JVM-level operation). To avoid this exception,
     * make sure that the server stops running (i.e., current "run" method ends) before
     * closing the socket. See {@link #closeSocket()} for details
     *
     * @param t
     */
    void handleRunningThrowable(Throwable t);
}
