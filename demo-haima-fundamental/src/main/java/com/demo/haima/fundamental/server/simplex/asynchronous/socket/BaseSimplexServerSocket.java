package com.demo.haima.fundamental.server.simplex.asynchronous.socket;

import java.nio.channels.AsynchronousSocketChannel;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;

/**
 * @author Vince Yuan
 * @date 2021/11/20
 */
public interface BaseSimplexServerSocket {

    /************************ Setup and Shutdown ************************/

    /**
     * This method is used to start the socket
     */
    void startSocket();

    /**
     * This method is used to close the socket.
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
     * This method is used to process throwable during socket running. <br/>
     * For {@link ExecutionException}, in {@link AioBaseSimplexServerSocket}, when {@link #closeSocket()} executes in the
     * execution of {@link Future#get()} method of future result of server socket channel's accept in-between two
     * {@link AioBaseSimplexServerSocket#serverSocketChannel}'s {@link AsynchronousSocketChannel#isOpen()} in the while-loop
     * in {@link AioBaseSimplexServerSocket#runWithFuture()}, it will cause this exception, while it does not affect server
     * processing data that has been received. Hence, it can be ignored in this situation.
     *
     * @param t
     */
    void handleRunningThrowable(Throwable t);
}
