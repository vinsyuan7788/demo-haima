package com.demo.haima.fundamental.client.simplex.nonblocking.socket;

/**
 * @author Vince Yuan
 * @date 2021/11/20
 */
public interface DataSimplexClientSocket extends BaseSimplexClientSocket {

    /**
     * This method is used to submit the data to send to server
     *
     * @param data
     */
    <T> void submit(T data);
}
