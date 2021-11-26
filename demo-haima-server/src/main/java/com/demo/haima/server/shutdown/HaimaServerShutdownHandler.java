package com.demo.haima.server.shutdown;

import com.demo.haima.server.state.ServerState;

import java.util.concurrent.CountDownLatch;

/**
 * @author Vince Yuan
 * @date 2021/11/9
 */
public class HaimaServerShutdownHandler implements ServerShutdownHandler {

    private final CountDownLatch shutdownLatch;

    public HaimaServerShutdownHandler(CountDownLatch shutdownLatch) {
        this.shutdownLatch = shutdownLatch;
    }

    @Override
    public void handle(ServerState serverState) {
        if (serverState == ServerState.ERROR || serverState == ServerState.SHUTDOWN) {
            shutdownLatch.countDown();
        }
    }
}
