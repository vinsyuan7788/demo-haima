package com.demo.haima.fundamental.client.simplex.nonblocking.socket;

import com.demo.haima.fundamental.utils.data.network.base.packet.Packet;

/**
 * @author Vince Yuan
 * @date 2021/11/20
 */
public interface PacketSimplexClientSocket extends BaseSimplexClientSocket {

    /**
     * This method is used to submit the packet to send to server
     *
     * @param packet
     */
    void submit(Packet<?> packet);
}
