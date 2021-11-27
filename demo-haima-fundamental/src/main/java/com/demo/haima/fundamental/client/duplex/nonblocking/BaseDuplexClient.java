package com.demo.haima.fundamental.client.duplex.nonblocking;

/**
 * @author Vince Yuan
 * @date 2021/11/20
 */
public abstract class BaseDuplexClient {

    /**
     * This method is used to start the client
     */
    public abstract void startClient();

    /**
     * This method is used to close the client
     */
    public abstract void closeClient();
}
