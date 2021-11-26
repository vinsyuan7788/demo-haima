package com.demo.haima.server.listener;

import com.demo.haima.server.HaimaServer;
import com.demo.haima.server.state.ServerState;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Default listener implementation, which will be used to notify internal
 * errors. For example, if some critical thread has stopped due to fatal errors,
 * then it will get notifications and will change the state of haima server
 * to ERROR representing an error status.
 *
 * @author Vince Yuan
 * @date 2021/11/10
 */
public class HaimaServerListener implements ServerListener {

    private static final Logger LOG = LoggerFactory.getLogger(HaimaServerListener.class);

    private final HaimaServer haimaServer;

    public HaimaServerListener(HaimaServer haimaServer) {
        this.haimaServer = haimaServer;
    }

    @Override
    public void notifyStopping(String threadName, int errorCode) {
        LOG.info("Thread {} exits, error code {}", threadName, errorCode);
        haimaServer.setServerState(ServerState.ERROR);
    }
}
