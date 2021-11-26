package com.demo.haima.server.session.tracker;

/**
 * This is the basic interface that Haima uses to track sessions.
 *
 * @author Vince Yuan
 * @date 2021/11/9
 */
public interface SessionTracker {

    /**
     * This method is used to add a session with its ID and timeout
     *
     * @param sessionId
     * @param sessionTimeout
     */
    void addSession(long sessionId, int sessionTimeout);

    /**
     * This method is used to touch a session by its ID and timeout
     *
     * @param sessionId
     * @param sessionTimeout
     * @return true if a session is touched, otherwise false if a session is no longer active
     */
    boolean touchSession(long sessionId, int sessionTimeout);

    /**
     * This method is used to mark that the session is in the process of closing.
     *
     * @param sessionId
     */
    void setSessionClosing(long sessionId);

    /**
     * This method is used to create a session
     *
     * @param sessionTimeout
     * @return
     */
    long createSession(int sessionTimeout);

    /**
     * This method is used to check the session with the specific owner <br/>
     * If the session does not have an owner, then assign the owner to this session,
     * otherwise check if the session's owner is the specific owner
     *
     * @param sessionId
     * @param owner
     */
    void checkSession(long sessionId, Object owner) throws Exception;

    /**
     * This method is used to remove a session
     *
     * @param sessionId
     */
    void removeSession(long sessionId);

    /**
     * This method is used to shutdown the session tracker
     */
    void shutdown();
}
