package com.demo.haima.server.session;

/**
 * @author Vince Yuan
 * @date 2021/11/11
 */
public interface Session {

    /**
     * This method is used to get session ID
     *
     * @return
     */
    long getSessionId();

    /**
     * This method is used to get session timeout (milliseconds)
     *
     * @return
     */
    int getTimeout();

    /**
     * This method is used to get session expiration time
     *
     * @return
     */
    long getExpirationTime();

    /**
     * This method is used to tell if the session is closing
     *
     * @return
     */
    boolean isClosing();
}
