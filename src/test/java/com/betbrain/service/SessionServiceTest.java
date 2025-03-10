package com.betbrain.service;

import com.betbrain.model.Session;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.Test;
import static org.junit.Assert.*;
import java.lang.reflect.Field;
import java.util.concurrent.ConcurrentHashMap;

public class SessionServiceTest {
    private SessionService sessionService;

    @Before
    public void setUp() throws Exception {
        sessionService = SessionService.getInstance();
        resetSingletonState();
    }

    @AfterClass
    public static void tearDown() {
        SessionService.shutdown();
    }

    @Test
    public void testGetOrCreateSessionCreatesNewSessionForNewCustomer() {
        String sessionKey = sessionService.getOrCreateSession(1);
        assertNotNull(sessionKey);
    }

    @Test
    public void testGetOrCreateSessionReusesValidSession() {
        String initialKey = sessionService.getOrCreateSession(1);
        String subsequentKey = sessionService.getOrCreateSession(1);
        assertEquals(initialKey, subsequentKey);
    }

    @Test
    public void testIsValidSessionReturnsFalseForExpiredSession() throws Exception {
        Session expiredSession = new Session("expired", System.currentTimeMillis() - 1);
        forceAddSession(1, expiredSession);

        assertFalse(sessionService.isValidSession("expired"));
    }

    @Test
    public void testCleanExpiredSessionsRemovesOldEntries() throws Exception {
        Session validSession = new Session("valid", System.currentTimeMillis() + 10000);
        Session expiredSession = new Session("expired", System.currentTimeMillis() - 1);
        forceAddSession(1, validSession);
        forceAddSession(2, expiredSession);

        sessionService.cleanExpiredSessions();
        assertEquals(1, getActiveSessionCount());
    }

    private void resetSingletonState() throws Exception {
        Field field = SessionService.class.getDeclaredField("activeSessions");
        field.setAccessible(true);
        ((ConcurrentHashMap) field.get(null)).clear();
    }

    private void forceAddSession(int customerId, Session session) throws Exception {
        Field field = SessionService.class.getDeclaredField("activeSessions");
        field.setAccessible(true);
        ((ConcurrentHashMap) field.get(null)).put(customerId, session);
    }

    private int getActiveSessionCount() throws Exception {
        Field field = SessionService.class.getDeclaredField("activeSessions");
        field.setAccessible(true);
        return ((ConcurrentHashMap) field.get(null)).size();
    }
}
