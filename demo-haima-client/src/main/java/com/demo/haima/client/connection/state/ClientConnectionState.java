package com.demo.haima.client.connection.state;

/**
 * @author Vince Yuan
 * @date 2021/11/12
 */
public enum ClientConnectionState {

    CONNECTING,
    CONNECTED,
    CONNECTED_READONLY,
    CLOSED,
    NOT_CONNECTED,
    ;
    public boolean isAlive() {
        return this != CLOSED;
    }

    /**
     * Returns whether we are connected to a server (which
     * could possibly be read-only, if this client is allowed
     * to go to read-only mode)
     * */
    public boolean isConnected() {
        return this == CONNECTED || this == CONNECTED_READONLY;
    }
}
