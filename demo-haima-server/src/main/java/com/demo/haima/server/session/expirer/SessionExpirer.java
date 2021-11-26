package com.demo.haima.server.session.expirer;

import com.demo.haima.server.session.Session;

/**
 * @author Vince Yuan
 * @date 2021/11/9
 */
public interface SessionExpirer {

    /**
     * This method is used to expire the session
     *
     * @param session
     */
    void expire(Session session);

    /**
     * This method is used to get the server ID
     *
     * @return
     */
    long getServerId();
}
