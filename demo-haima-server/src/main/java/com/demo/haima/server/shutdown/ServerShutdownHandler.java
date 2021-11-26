package com.demo.haima.server.shutdown;

import com.demo.haima.server.state.ServerState;

/**
 * @author Vince Yuan
 * @date 2021/11/9
 */
public interface ServerShutdownHandler {

    /**
     * This will be invoked when the server transition to a new server state.
     *
     * @param serverState new server state
     */
    void handle(ServerState serverState);
}
