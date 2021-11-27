package com.demo.haima.fundamental.utils.provider;

import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;

/**
 * @author Vince Yuan
 * @date 2021/11/10
 */
public interface SocketChannelInfoProvider {

    /**
     * This method is used to log the information regarding socket channel for utility-use
     *
     * @param socketChannel
     * @throws Exception
     */
    void logSocketChannelInfo(SocketChannel socketChannel) throws Exception;

    /**
     * This method is used to log the information regarding server socket channel for utility-use
     *
     * @param serverSocketChannel
     * @throws Exception
     */
    void logServerSocketChannelInfo(ServerSocketChannel serverSocketChannel) throws Exception;
}
