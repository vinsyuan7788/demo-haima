package com.demo.haima.test.integration.demo.communication.nonblocking.simplex.client;

import com.demo.haima.test.integration.demo.utils.state.client.nio.ClientState;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author Vince Yuan
 * @date 2021/11/21
 */
public abstract class SerdesSimplexClient extends BaseSimplexClient {

    private static final Logger LOG = LoggerFactory.getLogger(SerdesSimplexClient.class);

    public ClientState clientState;

    public void setClientState(ClientState clientState) {
        this.clientState = clientState;
        LOG.info("Client state is set to {}", clientState);
    }

    public ClientState getClientState() {
        return clientState;
    }

    /**
     * This method is used to write data to server
     *
     * @param data
     * @param <T>
     */
    public abstract <T> void write(T data);
}
