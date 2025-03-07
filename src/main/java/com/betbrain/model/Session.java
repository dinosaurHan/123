package com.betbrain.model;

/**
 * Represents a user session with authentication details
 */
public class Session {
    /**
     * Unique session identifier for authentication
     */
    private final String sessionKey;

    /**
     * Expiration timestamp in milliseconds since epoch
     */
    private final long expireTime;

    /**
     * Creates a new session instance
     * @param sessionKey Unique session authentication key
     * @param expireTime Session expiration timestamp (milliseconds)
     */
    public Session(String sessionKey, long expireTime) {
        this.sessionKey = sessionKey;
        this.expireTime = expireTime;
    }

    /**
     * @return Session authentication key
     */
    public String getSessionKey() {
        return sessionKey;
    }

    /**
     * @return Expiration timestamp in milliseconds
     */
    public long getExpireTime() {
        return expireTime;
    }
}