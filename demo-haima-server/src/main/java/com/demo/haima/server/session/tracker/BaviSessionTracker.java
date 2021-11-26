package com.demo.haima.server.session.tracker;

import com.demo.haima.common.exception.HaimaException;
import com.demo.haima.common.utility.LogUtils;
import com.demo.haima.server.exception.exiter.UncaughtExceptionExiter;
import com.demo.haima.server.listener.ServerListener;
import com.demo.haima.server.session.BaviSession;
import com.demo.haima.server.session.expirer.SessionExpirer;
import com.demo.haima.server.session.set.BaviSessionSet;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * This is a full featured SessionTracker. It tracks session in grouped by tick
 * interval. It always rounds up the tick interval to provide a sort of grace
 * period. Sessions are thus expired in batches made up of sessions that expire
 * in a given interval.
 *
 * @author Vince Yuan
 * @date 2021/11/11
 */
public class BaviSessionTracker extends UncaughtExceptionExiter implements SessionTracker {

    private static final Logger LOG = LoggerFactory.getLogger(BaviSessionTracker.class);

    private SessionExpirer sessionExpirer;
    private Map<Long, Integer> sessionIdAndSessionTimeoutMap;
    private int expirationInterval;
    private long expirationTime;
    /**
     *  A session ID maintained by this tracker,
     *  this ID will be used when creating a new session
     */
    private long sessionId;
    private Map<Long, BaviSession> sessionIdAndSessionMap = new HashMap<>();
    private Map<Long, BaviSessionSet> sessionExpirationTimeAndSessionSetMap = new HashMap<>();

    private volatile boolean running;
    private volatile long currentTime;

    public BaviSessionTracker(SessionExpirer sessionExpirer, ConcurrentHashMap<Long, Integer> sessionIdAndSessionTimeoutMap, int expirationInterval, long sid, ServerListener serverListener) {
        super("SessionTracker", serverListener);
        this.sessionExpirer = sessionExpirer;
        LOG.info(LogUtils.getMessage("Session expirer is set"));
        this.sessionIdAndSessionTimeoutMap = sessionIdAndSessionTimeoutMap;
        this.expirationInterval = expirationInterval;
        LOG.info(LogUtils.getMessage("Expiration interval is set to {}ms"), this.expirationInterval);
        this.expirationTime = roundToInterval(System.currentTimeMillis());
        LOG.info(LogUtils.getMessage("Expiration time is set to {}ms"), this.expirationTime);
        this.sessionId = initializeSessionId(sid);
        LOG.info(LogUtils.getMessage("Session ID is set to {}"), this.sessionId);
        for (Map.Entry<Long, Integer> e : sessionIdAndSessionTimeoutMap.entrySet()) {
            addSession(e.getKey(), e.getValue());
        }
    }

    @Override
    public synchronized void run() {
        LOG.info(LogUtils.getMessage("Session tracker thread starts running"));
        running = true;
        try {
            while (running) {
                currentTime = System.currentTimeMillis();
                if (expirationTime > currentTime) {
                    this.wait(expirationTime - currentTime);
                    continue;
                }
                BaviSessionSet set = sessionExpirationTimeAndSessionSetMap.remove(expirationTime);
                if (set != null) {
                    for (BaviSession s : set.sessions) {
                        setSessionClosing(s.getSessionId());
                        sessionExpirer.expire(s);
                    }
                }
                expirationTime += expirationInterval;
            }
        } catch (InterruptedException e) {
            handleException(this.getName(), e);
        }
        LOG.info(LogUtils.getMessage("Session tracker thread completes running"));
    }

    @Override
    public synchronized void addSession(long sessionId, int sessionTimeout) {
        sessionIdAndSessionTimeoutMap.put(sessionId, sessionTimeout);
        if (sessionIdAndSessionMap.get(sessionId) == null) {
            BaviSession s = new BaviSession(sessionId, sessionTimeout, 0);
            sessionIdAndSessionMap.put(sessionId, s);
            // todo v.y. trace
//            if (LOG.isTraceEnabled()) {
//                BETrace.logTraceMessage(LOG, BETrace.SESSION_TRACE_MASK,
//                        "SessionTrackerImpl --- Adding session 0x"
//                                + Long.toHexString(id) + " " + sessionTimeout);
//            }
        } else {
            // todo v.y. trace
//            if (LOG.isTraceEnabled()) {
//                BETrace.logTraceMessage(LOG, BETrace.SESSION_TRACE_MASK,
//                        "SessionTrackerImpl --- Existing session 0x"
//                                + Long.toHexString(id) + " " + sessionTimeout);
//            }
        }
        touchSession(sessionId, sessionTimeout);
    }

    @Override
    public synchronized boolean touchSession(long sessionId, int sessionTimeout) {
        // todo v.y. trace
//        if (LOG.isTraceEnabled()) {
//            BETrace.logTraceMessage(LOG,
//                    BETrace.CLIENT_PING_TRACE_MASK,
//                    "SessionTrackerImpl --- Touch session: 0x"
//                            + Long.toHexString(sessionId) + " with timeout " + timeout);
//        }
        BaviSession s = sessionIdAndSessionMap.get(sessionId);
        // Return false, if the session doesn't exists or marked as closing
        if (s == null || s.isClosing()) {
            return false;
        }
        long expirationTime = roundToInterval(System.currentTimeMillis() + sessionTimeout);
        if (s.getExpirationTime() >= expirationTime) {
            // Nothing needs to be done
            return true;
        }
        BaviSessionSet set = sessionExpirationTimeAndSessionSetMap.get(s.getExpirationTime());
        if (set != null) {
            set.sessions.remove(s);
        }
        s.setExpirationTime(expirationTime);
        set = sessionExpirationTimeAndSessionSetMap.get(s.getExpirationTime());
        if (set == null) {
            set = new BaviSessionSet();
            sessionExpirationTimeAndSessionSetMap.put(expirationTime, set);
        }
        set.sessions.add(s);
        return true;
    }

    @Override
    public void setSessionClosing(long sessionId) {
        if (LOG.isTraceEnabled()) {
            LOG.info("Session closing: 0x" + Long.toHexString(sessionId));
        }
        BaviSession s = sessionIdAndSessionMap.get(sessionId);
        if (s == null) {
            return;
        }
        s.isClosing(true);
    }

    @Override
    public long createSession(int sessionTimeout) {
        addSession(sessionId, sessionTimeout);
        return sessionId++;
    }

    @Override
    public synchronized void checkSession(long sessionId, Object owner) throws Exception {
        BaviSession session = sessionIdAndSessionMap.get(sessionId);
        if (session == null || session.isClosing()) {
            throw new HaimaException.SessionExpiredException();
        }
        if (session.owner == null) {
            session.owner = owner;
        } else if (session.owner != owner) {
            throw new HaimaException.SessionMovedException();
        }
    }

    @Override
    public void removeSession(long sessionId) {
        BaviSession s = sessionIdAndSessionMap.remove(sessionId);
        sessionIdAndSessionTimeoutMap.remove(sessionId);
        if (LOG.isTraceEnabled()) {
            // todo v.y. trace
//            BETrace.logTraceMessage(LOG, BETrace.SESSION_TRACE_MASK,
//                    "SessionTrackerImpl --- Removing session 0x"
//                            + Long.toHexString(sessionId));
        }
        if (s != null) {
            BaviSessionSet set = sessionExpirationTimeAndSessionSetMap.get(s.getExpirationTime());
            // Session expiration has been removing the sessions
            if(set != null){
                set.sessions.remove(s);
            }
        }
    }

    @Override
    public void shutdown() {
        running = false;
        if (LOG.isTraceEnabled()) {
            // todo v.y. trace
//            BETrace.logTraceMessage(LOG, BETrace.getTextTraceLevel(),
//                    "Shutdown SessionTrackerImpl!");
        }
        LOG.info(LogUtils.getMessage("Session tracker is shut down"));
    }

    private long roundToInterval(long currentTimeMillis) {
        // We give a one interval grace period
        return (currentTimeMillis / expirationInterval + 1) * expirationInterval;
    }

    /**
     * This method is used to initialize session ID
     *
     * @param sid
     * @return
     */
    private long initializeSessionId(long sid) {
        long nextSid = 0;
        nextSid = (System.currentTimeMillis() << 24) >>> 8;
        nextSid =  nextSid | (sid <<56);
        return nextSid;
    }
}
