package com.demo.haima.common.exception.logger;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This is the main class for catching all the uncaught exceptions thrown by the threads.
 *
 * @author Vince Yuan
 * @date 2021/11/10
 */
public class UncaughtExceptionLogger extends Thread {

    private static final Logger LOG = LoggerFactory.getLogger(UncaughtExceptionLogger.class);

    private UncaughtExceptionHandler uncaughtExceptionalHandler = (t, e) -> handleException(t.getName(), e);

    public UncaughtExceptionLogger(Runnable thread, String threadName) {
        super(thread, threadName);
        setUncaughtExceptionHandler(uncaughtExceptionalHandler);
    }

    public UncaughtExceptionLogger(String threadName) {
        super(threadName);
        setUncaughtExceptionHandler(uncaughtExceptionalHandler);
    }

    /**
     * This method will be used by the uncaught exception handler and just log a
     * warning message and return.
     *
     * @param threadName - thread name
     * @param throwable - throwable object
     */
    protected void handleException(String threadName, Throwable throwable) {
        LOG.warn("Exception occurred from thread {}", threadName, throwable);
    }
}
