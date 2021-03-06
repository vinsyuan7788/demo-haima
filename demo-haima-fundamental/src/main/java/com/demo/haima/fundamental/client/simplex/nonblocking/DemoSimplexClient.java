package com.demo.haima.fundamental.client.simplex.nonblocking;

import com.demo.haima.fundamental.utility.state.client.nonblocking.ClientState;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author Vince Yuan
 * @date 2021/11/21
 */
public abstract class DemoSimplexClient extends BaseSimplexClient {

    private static final Logger LOG = LoggerFactory.getLogger(DemoSimplexClient.class);

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
