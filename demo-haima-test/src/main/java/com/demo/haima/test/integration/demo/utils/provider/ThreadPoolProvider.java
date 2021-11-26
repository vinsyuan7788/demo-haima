package com.demo.haima.test.integration.demo.utils.provider;

import java.util.concurrent.ExecutorService;

/**
 * @author Vince Yuan
 * @date 2021/11/25
 */
public interface ThreadPoolProvider {

    /**
     * This method is used to get a default cached thread pool implemented by executors
     *
     * @param threadName
     * @return
     */
    ExecutorService getDefaultCachedThreadPool(String threadName);
}
