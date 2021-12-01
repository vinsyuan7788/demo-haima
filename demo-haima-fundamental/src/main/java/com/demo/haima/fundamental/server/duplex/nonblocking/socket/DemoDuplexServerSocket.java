package com.demo.haima.fundamental.server.duplex.nonblocking.socket;

import com.demo.haima.fundamental.utility.data.network.packet.Packet;

import java.nio.channels.SelectionKey;

/**
 * @author Vince Yuan
 * @date 2021/11/21
 */
public interface DemoDuplexServerSocket extends BaseDuplexServerSocket {

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
    void processConnectEvent(SelectionKey selectedKey) throws Exception;

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
    void processWriteEvent(SelectionKey selectedKey) throws Exception;

    /**
     * This method is used to process the received packet and return a new one
     *
     * @param receivedPacket
     * @return
     */
    Packet processReceivedPacket(Packet receivedPacket);
}
