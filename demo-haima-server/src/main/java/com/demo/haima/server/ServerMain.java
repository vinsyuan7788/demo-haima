package com.demo.haima.server;

import com.demo.haima.common.utility.LogUtils;
import com.demo.haima.server.connection.factory.ServerConnectionFactory;
import com.demo.haima.server.shutdown.HaimaServerShutdownHandler;
import com.demo.haima.server.snowflake.factory.SnowFlakeIdWorkerFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.CountDownLatch;

/**
 *
 * @author Vince Yuan
 * @date 05/27/2021
 */
public class ServerMain {

    private static final Logger LOG = LoggerFactory.getLogger(ServerMain.class);

    public static void main(String[] args) {
        ServerMain main = new ServerMain();
        main.initializeAndRun(args);
    }

    private void initializeAndRun(String[] args) {
        LOG.info(LogUtils.getMessage("Server is starting"));
        try {
            // todo v.y. config
//            SnowFlakeIdWorkerFactory.init(config.snowFlakeWorkerLimit, config.serverId - 1, config.snowFlakeDatacenterId);
            SnowFlakeIdWorkerFactory.init(5, 10, 2);

            Server server = new HaimaServer();
            LOG.info(LogUtils.getMessage("Server is instantiated"));

            final CountDownLatch shutdownLatch = new CountDownLatch(1);
            HaimaServerShutdownHandler serverShutdownHandler = new HaimaServerShutdownHandler(shutdownLatch);
            LOG.info(LogUtils.getMessage("Server shutdown handler is instantiated"));

            server.registerServerShutdownHandler(serverShutdownHandler);
            // todo v.y. config
//            server.setTickTime(config.tickTime);
//            server.setMinSessionTimeout(config.minSessionTimeout);
//            server.setMaxSessionTimeout(config.maxSessionTimeout);
            server.setTickTime(3000);
            server.setMinSessionTimeout(-1);
            server.setMaxSessionTimeout(-1);

            ServerConnectionFactory serverConnectionFactory = ServerConnectionFactory.createFactory();
            LOG.info(LogUtils.getMessage("Server connection factory is instantiated"));

            serverConnectionFactory.configure(null, null);
            serverConnectionFactory.startup(server);

            shutdownLatch.await();

            serverConnectionFactory.shutdown();
            if (server.canShutdown()) {
                server.shutdown(true);
            }
        } catch (Exception e) {
            // todo v.y. ex
        }
    }
}
