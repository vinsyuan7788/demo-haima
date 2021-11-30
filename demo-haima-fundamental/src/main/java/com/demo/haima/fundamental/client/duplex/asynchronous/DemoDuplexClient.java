package com.demo.haima.fundamental.client.duplex.asynchronous;

import com.demo.haima.fundamental.utility.state.client.asynchronous.ClientState;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author Vince Yuan
 * @date 2021/11/21
 */
public abstract class DemoDuplexClient extends BaseDuplexClient {

    private static final Logger LOG = LoggerFactory.getLogger(DemoDuplexClient.class);

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
     * @return
     */
    public abstract <T> Boolean write(T data);

    /**
     * This method is used to get a snowflake ID from server
     *
     * @param appCode
     * @return
     */
    public abstract Long getSnowflakeId(int appCode);
}
