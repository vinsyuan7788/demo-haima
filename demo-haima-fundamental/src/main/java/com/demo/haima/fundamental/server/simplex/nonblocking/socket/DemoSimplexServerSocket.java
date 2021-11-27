package com.demo.haima.fundamental.server.simplex.nonblocking.socket;

import com.demo.haima.fundamental.utils.data.network.packet.Packet;

import java.nio.channels.SelectionKey;

/**
 * @author Vince Yuan
 * @date 2021/11/21
 */
public interface DemoSimplexServerSocket extends BaseSimplexServerSocket {

    /**
     * This method is used to process accept event
     *
     * @param selectedKey
     * @throws Exception
     */
    void processAcceptEvent(SelectionKey selectedKey) throws Exception;

    /**
     * This method is used to process connect event
     *
     * @param selectedKey
     * @throws Exception
     */
    void processConnectableEvent(SelectionKey selectedKey) throws Exception;

    /**
     * This method is used to process read event
     *
     * @param selectedKey
     * @throws Exception
     */
    void processReadEvent(SelectionKey selectedKey) throws Exception;

    /**
     * This method is used to process write event
     *
     * @param selectedKey
     * @throws Exception
     */
    void processWritableEvent(SelectionKey selectedKey) throws Exception;

    /**
     * This method is used to process the received packet
     *
     * @param receivedPacket
     */
    void processReceivedPacket(Packet receivedPacket);
}
