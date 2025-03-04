package com.betbrain.service;

import com.betbrain.model.Session;
import com.betbrain.util.SessionKeyGenerator;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.logging.Logger;

/**
 * 会话业务逻辑处理
 *
 * @Date 2025.03.03
 * @Author Paul
 */
public class SessionService {

    private static final Logger log = Logger.getLogger("SessionService");
    private static final long SESSION_EXPIRY_TIME = 60 * 1000; // 10 minutes
    private static final ConcurrentHashMap<Integer, Session> sessions = new ConcurrentHashMap<>();//存储回话信息
    private static final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);

    private static final SessionService instance = new SessionService();

    private SessionService() {
        // 启动定时任务，每分钟清理一次过期会话
        scheduler.scheduleAtFixedRate(this::cleanExpiredSessions, 1, 1, TimeUnit.MINUTES);
    }


    public static SessionService getInstance() {
        return instance;
    }

    /**
     * 获取 or 创建会话密钥
     *
     * @param customerId
     * @return
     */
    public String getOrCreateSession(int customerId) {
        Session session = sessions.compute(customerId, (key, existingSession) -> {
            //如果存储介质中已经存在且未过期，则直接返回原会话密钥；否则生成新的并返回；
            if (existingSession == null || System.currentTimeMillis() > existingSession.getExpireTime()) {
                String sessionKey = SessionKeyGenerator.generate();
                long expiryTime = System.currentTimeMillis() + SESSION_EXPIRY_TIME;
                return new Session(sessionKey, expiryTime);
            }
            return existingSession;
        });

        return session.getSessionKey();
    }

    /**
     * 检查密钥是否有效
     *
     * @param sessionKey
     * @return
     */
    public boolean isValidSession(String sessionKey) {
        return sessions.values().stream()
                .anyMatch(session -> session.getSessionKey().equals(sessionKey) &&
                        System.currentTimeMillis() <= session.getExpireTime());
    }

    /**
     * 根据会话密钥获取客户id
     *
     * @param sessionKey
     * @return
     */
    public int getCustomerIdBySessionKey(String sessionKey) {
        return sessions.entrySet().stream()
                .filter(entry -> entry.getValue().getSessionKey().equals(sessionKey))
                .map(Map.Entry::getKey)
                .findFirst()
                .orElse(-1);
    }

    /**
     * 清理过期的会话密钥，防止内存溢出
     */
    public void cleanExpiredSessions() {
        long currentTime = System.currentTimeMillis();
        sessions.entrySet().removeIf(entry -> {
            if (entry.getValue().getExpireTime() <= currentTime) {
                System.out.println("session expired ,clearing...customer Id : " + entry.getKey() + ",session Key : " + entry.getValue().getSessionKey());
                return true;
            }
            return false;
        });
    }

    public static void shutdown() {
        scheduler.shutdown();
    }


}