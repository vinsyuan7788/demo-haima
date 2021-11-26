package com.demo.haima.test.integration.demo.communication.nonblocking.simplex.client.socket;

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
