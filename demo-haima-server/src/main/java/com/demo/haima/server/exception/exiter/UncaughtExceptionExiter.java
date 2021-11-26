package com.demo.haima.server.exception.exiter;

import com.demo.haima.common.shutdown.ExitCode;
import com.demo.haima.common.exception.logger.UncaughtExceptionLogger;
import com.demo.haima.server.listener.ServerListener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author Vince Yuan
 * @date 2021/11/10
 */
public class UncaughtExceptionExiter extends UncaughtExceptionLogger {

    private static final Logger LOG = LoggerFactory.getLogger(UncaughtExceptionExiter.class);

    private final ServerListener serverListener;

    public UncaughtExceptionExiter(String threadName, ServerListener serverListener) {
        super(threadName);
        this.serverListener = serverListener;
    }

    /**
     * This method will be used by the uncaught exception handler and just log a
     * warning message and notify the server to stop through the listener.
     *
     * @param threadName - thread name
     * @param throwable - throwable object
     */
    @Override
    protected void handleException(String threadName, Throwable throwable) {
        LOG.error("Severe unrecoverable error, from thread : {}", threadName, throwable);
        serverListener.notifyStopping(threadName, ExitCode.UNEXPECTED_ERROR.getCode());
    }
}
