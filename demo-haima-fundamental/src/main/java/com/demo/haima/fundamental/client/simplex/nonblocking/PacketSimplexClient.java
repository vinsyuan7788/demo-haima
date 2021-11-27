package com.demo.haima.fundamental.client.simplex.nonblocking;

/**
 * @author Vince Yuan
 * @date 2021/11/20
 */
public abstract class PacketSimplexClient extends BaseSimplexClient {

    /**
     * This method is used to write data to server
     *
     * @param data
     * @param <T>
     */
    public abstract <T> void write(T data);
}
