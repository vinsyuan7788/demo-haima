package com.demo.haima.test.integration.demo.utils.exception.handler;

/**
 * @author Vince Yuan
 * @date 2021/11/25
 */
public interface CompletionExceptionHandler {

    /**
     * This method is used to handle running throwable
     *
     * @param t
     */
    void handleRunningThrowable(Throwable t);
}
