package com.demo.haima.server.statistics.tracker;

import java.util.concurrent.atomic.AtomicInteger;

/**
 * @author Vince Yuan
 * @date 2021/11/11
 */
public interface ServerStatisticsTracker {

    AtomicInteger numberOfRequestInProcess = new AtomicInteger(0);

    /**
     * This method is used to increase the number of request in-process
     */
    void increaseNumberOfRequestInProcess();

    /**
     * This method is used to decrease the number of request in-process
     */
    void decreaseNumberOfRequestsInProcess();

    /**
     * This method is used to get the number of request in-process
     *
     * @return
     */
    int getNumberOfRequestInProcess();
}
