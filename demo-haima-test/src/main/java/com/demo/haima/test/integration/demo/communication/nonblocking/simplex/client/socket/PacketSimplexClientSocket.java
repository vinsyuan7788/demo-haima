package com.demo.haima.test.integration.demo.communication.nonblocking.simplex.client.socket;

import com.demo.haima.test.integration.demo.utils.data.network.base.packet.Packet;

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
