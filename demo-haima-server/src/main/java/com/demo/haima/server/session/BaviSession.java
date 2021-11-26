package com.demo.haima.server.session;

/**
 * @author Vince Yuan
 * @date 2021/11/11
 */
public class BaviSession implements Session {

    public Object owner;

    private final long sessionId;
    private final int timeout;
    private long expirationTime;
    private boolean isClosing;

    public BaviSession(long sessionId, int timeout, long expirationTime) {
        this.sessionId = sessionId;
        this.timeout = timeout;
        this.expirationTime = expirationTime;
        isClosing = false;
    }

    @Override
    public long getSessionId() { return sessionId; }

    @Override
    public int getTimeout() { return timeout; }

    @Override
    public long getExpirationTime() {
        return expirationTime;
    }

    @Override
    public boolean isClosing() { return isClosing; }

    public void setExpirationTime(long expirationTime) {
        this.expirationTime = expirationTime;
    }

    public void isClosing(boolean isClosing) {
        this.isClosing = isClosing;
    }
}
