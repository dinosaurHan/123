package com.betbrain.service;

import com.betbrain.model.Session;
import com.betbrain.util.SessionKeyGenerator;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Manages user session lifecycle and validation
 */
public class SessionService {
    private static final Logger logger = Logger.getLogger(SessionService.class.getName());
    private static final long DEFAULT_SESSION_TIMEOUT_MS = 600000; // 10 minutes
    private static final int SESSION_CLEANUP_INTERVAL = 1; // minutes

    private static final ConcurrentHashMap<Integer, Session> activeSessions = new ConcurrentHashMap<>();
    private static final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);
    private static final SessionService instance = new SessionService();

    private SessionService() {
        initializeSessionCleanup();
    }

    public static SessionService getInstance() {
        return instance;
    }

    /**
     * Retrieves or creates a session for the specified customer
     * @param customerId Unique customer identifier
     * @return Session key for authentication
     */
    public String getOrCreateSession(int customerId) {
        return activeSessions.compute(customerId, (key, existing) ->
                shouldRenewSession(existing) ? createNewSession() : existing
        ).getSessionKey();
    }

    /**
     * Validates session existence and expiration status
     * @param sessionKey Authentication token to validate
     * @return true if valid and non-expired session exists
     */
    public boolean isValidSession(String sessionKey) {
        return activeSessions.values().stream()
                .anyMatch(s -> s.getSessionKey().equals(sessionKey) && !isExpired(s));
    }

    /**
     * Retrieves customer ID associated with a session key
     * @param sessionKey Authentication token to lookup
     * @return Customer ID or -1 if not found
     */
    public int getCustomerIdBySessionKey(String sessionKey) {
        return activeSessions.entrySet().stream()
                .filter(e -> e.getValue().getSessionKey().equals(sessionKey))
                .map(Map.Entry::getKey)
                .findFirst()
                .orElse(-1);
    }

    /**
     * Initializes periodic session cleanup task
     */
    private void initializeSessionCleanup() {
        scheduler.scheduleAtFixedRate(
                this::cleanExpiredSessions,
                SESSION_CLEANUP_INTERVAL,
                SESSION_CLEANUP_INTERVAL,
                TimeUnit.MINUTES
        );
    }

    /**
     * Removes expired sessions from active session store
     */
    public void cleanExpiredSessions() {
        final long currentTime = System.currentTimeMillis();
        activeSessions.entrySet().removeIf(entry -> {
            if (isExpired(entry.getValue())) {
                logger.log(Level.FINE, "Clearing expired session: Customer={0}, Key={1}",
                        new Object[]{entry.getKey(), entry.getValue().getSessionKey()});
                return true;
            }
            return false;
        });
    }

    /**
     * Shuts down session maintenance tasks
     */
    public static void shutdown() {
        scheduler.shutdown();
        try {
            if (!scheduler.awaitTermination(5, TimeUnit.SECONDS)) {
                logger.warning("Force shutdown session cleanup tasks");
                scheduler.shutdownNow();
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    private boolean shouldRenewSession(Session existing) {
        return existing == null || isExpired(existing);
    }

    private boolean isExpired(Session session) {
        return System.currentTimeMillis() > session.getExpireTime();
    }

    private Session createNewSession() {
        return new Session(
                SessionKeyGenerator.generate(),
                System.currentTimeMillis() + DEFAULT_SESSION_TIMEOUT_MS
        );
    }
}