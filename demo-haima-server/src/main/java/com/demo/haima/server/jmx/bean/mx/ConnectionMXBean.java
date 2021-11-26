package com.demo.haima.server.jmx.bean.mx;

/**
 * This interface is used to represent an MX bean of connection from the client.
 * Any class that implements this bean can be registered to JMX for management.
 *
 * @author Vince Yuan
 * @date 2021/11/18
 */
public interface ConnectionMXBean {

    /**
     * This method is used to get the session ID of the client
     *
     * @return client's session id
     */
    String getSessionId();
}
