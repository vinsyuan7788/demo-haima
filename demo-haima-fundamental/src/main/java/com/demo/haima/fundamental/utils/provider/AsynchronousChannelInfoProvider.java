package com.demo.haima.fundamental.utils.provider;

import java.nio.channels.AsynchronousServerSocketChannel;
import java.nio.channels.AsynchronousSocketChannel;

/**
 * @author Vince Yuan
 * @date 2021/11/25
 */
public interface AsynchronousChannelInfoProvider {

    /**
     * This method is used to log the information regarding socket channel for utility-use
     *
     * @param socketChannel
     * @throws Exception
     */
    void logSocketChannelInfo(AsynchronousSocketChannel socketChannel) throws Exception;

    /**
     * This method is used to log the information regarding server socket channel for utility-use
     *
     * @param serverSocketChannel
     * @throws Exception
     */
    void logServerSocketChannelInfo(AsynchronousServerSocketChannel serverSocketChannel) throws Exception;
}
