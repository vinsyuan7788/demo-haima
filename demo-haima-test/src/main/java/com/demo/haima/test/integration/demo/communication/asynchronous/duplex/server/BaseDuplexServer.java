package com.demo.haima.test.integration.demo.communication.asynchronous.duplex.server;

/**
 * @author Vince Yuan
 * @date 2021/11/20
 */
public abstract class BaseDuplexServer {

    /**
     * This method is used to start the server
     */
    public abstract void startServer();

    /**
     * This method is used to close the server
     */
    public abstract void closeServer();
}
