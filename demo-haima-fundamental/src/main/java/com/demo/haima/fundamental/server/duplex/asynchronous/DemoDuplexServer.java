package com.demo.haima.fundamental.server.duplex.asynchronous;

import com.demo.haima.fundamental.utility.state.server.asynchronous.ServerState;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author Vince Yuan
 * @date 2021/11/21
 */
public abstract class DemoDuplexServer extends BaseDuplexServer {

    private static final Logger LOG = LoggerFactory.getLogger(DemoDuplexServer.class);

    private ServerState serverState;

    public void setServerState(ServerState serverState) {
        this.serverState = serverState;
        LOG.info("Server state is set to {}", serverState);
    }

    public ServerState getServerState() {
        return serverState;
    }
}
