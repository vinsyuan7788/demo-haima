package com.demo.haima.test.integration.demo.communication.nonblocking.simplex.server.socket;

import java.nio.channels.SelectionKey;

/**
 * @author Vince Yuan
 * @date 2021/11/20
 */
public interface PacketSimplexServerSocket extends BaseSimplexServerSocket {

    /**
     * This method is used to process accept event
     *
     * @param selectedKey
     * @throws Exception
     */
    void processAcceptEvent(SelectionKey selectedKey) throws Exception;

    /**
     * This method is used to process read event
     *
     * @param selectedKey
     * @throws Exception
     */
    void processReadEvent(SelectionKey selectedKey) throws Exception;
}
