package com.betbrain.model;

/**
 * 会话实体
 *
 * @Date 2025.03.03
 * @Author Paul
 */
public class Session {

    private final String sessionKey;

    private final long expireTime;

    public Session(String sessionKey, long expireTime) {
        this.sessionKey = sessionKey;
        this.expireTime = expireTime;
    }


    public String getSessionKey() {
        return sessionKey;
    }

    public long getExpireTime() {
        return expireTime;
    }
}
