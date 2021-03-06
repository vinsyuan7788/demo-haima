package com.demo.haima.fundamental.server.duplex.asynchronous.socket;

/**
 * @author Vince Yuan
 * @date 2021/11/24
 */
public interface BaseDuplexServerSocket {

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
     * This method is used to process throwable during socket running
     *
     * @param t
     */
    void handleRunningThrowable(Throwable t);
}
