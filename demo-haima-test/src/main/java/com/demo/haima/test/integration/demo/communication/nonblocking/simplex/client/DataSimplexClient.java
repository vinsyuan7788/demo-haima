package com.demo.haima.test.integration.demo.communication.nonblocking.simplex.client;

/**
 * @author Vince Yuan
 * @date 2021/11/20
 */
public abstract class DataSimplexClient extends BaseSimplexClient {

    /**
     * This method is used to write data to server
     *
     * @param data
     * @param <T>
     */
    public abstract <T> void write(T data);
}
