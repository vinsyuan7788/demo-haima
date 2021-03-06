package com.demo.haima.fundamental.client.duplex.asynchronous.socket;

import com.demo.haima.fundamental.utility.data.network.packet.Packet;
import com.demo.haima.fundamental.utility.data.network.response.body.ResponseBody;

import java.nio.channels.AsynchronousSocketChannel;

/**
 * @author Vince Yuan
 * @date 2021/11/21
 */
public interface DemoDuplexClientSocket extends BaseDuplexClientSocket {

    /****************************** Data Processing ******************************/

    /**
     * This method is used to submit the packet to send to server
     *
     * @param packet
     * @return
     */
    ResponseBody submit(Packet packet);

    /****************************** Setup and Shutdown ******************************/

    /**
     * This method is used to close the socket channel
     *
     * @param socketChannel
     */
    void closeSocketChannel(AsynchronousSocketChannel socketChannel);

    /**
     * This method is used to handle the throwable during closing socket channel
     *
     * @param t
     */
    void handleCloseSocketChannelThrowable(Throwable t);
}
