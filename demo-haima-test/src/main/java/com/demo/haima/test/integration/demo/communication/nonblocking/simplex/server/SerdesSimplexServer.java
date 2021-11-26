package com.demo.haima.test.integration.demo.communication.nonblocking.simplex.server;

import com.demo.haima.test.integration.demo.utils.state.server.nio.ServerState;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author Vince Yuan
 * @date 2021/11/21
 */
public abstract class SerdesSimplexServer extends BaseSimplexServer {

    private static final Logger LOG = LoggerFactory.getLogger(SerdesSimplexServer.class);

    private ServerState serverState;

    public void setServerState(ServerState serverState) {
        this.serverState = serverState;
        LOG.info("Server state is set to {}", serverState);
    }

    public ServerState getServerState() {
        return serverState;
    }
}
