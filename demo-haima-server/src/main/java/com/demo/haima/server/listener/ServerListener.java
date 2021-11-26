package com.demo.haima.server.listener;

/**
 * Listener for the critical events (e.g., resource events)
 *
 * @author Vince Yuan
 * @date 2021/11/10
 */
public interface ServerListener {

    /**
     * This will notify the server that some critical thread has stopped. It
     * usually takes place when fatal error occurred.
     *
     * @param threadName
     *            - name of the thread
     * @param errorCode
     *            - error code
     */
    void notifyStopping(String threadName, int errorCode);
}
