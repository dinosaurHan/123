package com.betbrain;

import org.junit.*;

import static org.junit.Assert.*;

import java.net.*;
import java.io.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.*;

/**
 * 基础逻辑测试
 */
public class BasicLogicTest {
    private static final String BASE_URL = "http://localhost:8001";
    private static final int SESSION_TIMEOUT_SEC = 60 * 10; // 10分钟会话超时
    private static final int TEST_CUSTOMER_ID = 9001;
    private static final int TEST_BET_ID = 10001;

    // ================= HTTP工具方法 =================
    private static String getSession(int customerId) throws IOException {
        HttpURLConnection conn = (HttpURLConnection) new URL(BASE_URL + "/" + customerId + "/session").openConnection();
        try (BufferedReader br = new BufferedReader(new InputStreamReader(conn.getInputStream()))) {
            return br.readLine();
        }
    }

    private static int postStake(int betId, String sessionKey, int amount) throws IOException {
        HttpURLConnection conn = (HttpURLConnection) new URL(BASE_URL + "/" + betId + "/stake?sessionkey=" + sessionKey).openConnection();
        conn.setRequestMethod("POST");
        conn.setDoOutput(true);
        try (OutputStream os = conn.getOutputStream()) {
            os.write(Integer.toString(amount).getBytes());
        }
        return conn.getResponseCode();
    }

    private static String getHighStakes(int betId) throws IOException {
        HttpURLConnection conn = (HttpURLConnection) new URL(BASE_URL + "/" + betId + "/highstakes").openConnection();
        try (BufferedReader br = new BufferedReader(new InputStreamReader(conn.getInputStream()))) {
            return br.lines().collect(Collectors.joining());
        }
    }

    // ================= 测试用例集 =================

    // ---------- 会话管理测试 ----------
    @Test
    public void testSessionKeyFormat() throws Exception {
        String sessionKey = getSession(TEST_CUSTOMER_ID);
        // 验证7位字母数字组合
        assertTrue("会话密钥应为7位字母数字", sessionKey.matches("^[A-Za-z0-9]{7}$"));
    }

    // ---------- 投注功能测试 ----------
    @Test
    public void testNormalWorkflow() throws Exception {
        // 完整业务流程测试
        String sessionKey = getSession(TEST_CUSTOMER_ID + 1);
        int statusCode = postStake(TEST_BET_ID + 1, sessionKey, 1500);
        assertEquals(200, statusCode);

        // 验证高额列表
        String result = getHighStakes(TEST_BET_ID + 1);
        assertTrue(result.contains(String.valueOf(TEST_CUSTOMER_ID + 1) + "=1500"));
    }

    // ---------- 高额投注列表测试 ----------
    @Test
    public void testEmptyHighStakes() throws Exception {
        int nonExistBetId = 99999;
        String response = getHighStakes(nonExistBetId);
        assertEquals("No stakes for bet ID: " + nonExistBetId, response);
    }

    @Test
    public void testTop20Logic() throws Exception {
        int betId = TEST_BET_ID + 2;
        // 生成25个不同客户的投注
        IntStream.range(0, 25).forEach(i -> {
            try {
                String sessionKey = getSession(TEST_CUSTOMER_ID + 100 + i);
                postStake(betId, sessionKey, 1000 + i);
            } catch (Exception e) { /* 处理异常 */ }
        });

        // 验证列表
        String result = getHighStakes(betId);
        String[] entries = result.split(",");
        System.out.println(result);
        assertEquals(20, entries.length);
        assertTrue("应包含最高金额1024", result.contains("1024"));
    }

    // ---------- 会话过期测试 ----------
    @Test
    public void testExpiredSession() throws Exception {
        String sessionKey = getSession(TEST_CUSTOMER_ID + 2);
        TimeUnit.SECONDS.sleep(SESSION_TIMEOUT_SEC + 1); // 超时1秒
        int statusCode = postStake(TEST_BET_ID + 2, sessionKey, 2000);
        assertEquals(401, statusCode);
    }

    // ---------- 并发测试 ----------
    @Test(timeout = 60000)
    public void testConcurrentOperations() throws Exception {
        final int CONCURRENCY = 100;
        final int BET_ID = TEST_BET_ID + 3;
        final AtomicInteger successCount = new AtomicInteger();
        final CountDownLatch latch = new CountDownLatch(CONCURRENCY);

        ExecutorService pool = Executors.newFixedThreadPool(CONCURRENCY);

        // 并发创建会话和投注
        IntStream.range(0, CONCURRENCY).forEach(i -> {
            pool.execute(() -> {
                try {
                    int customerId = TEST_CUSTOMER_ID + 200 + i;
                    String sessionKey = getSession(customerId);
                    if (postStake(BET_ID, sessionKey, 5000) == 200) {
                        successCount.incrementAndGet();
                    }
                } catch (Exception e) {
                    fail("并发操作失败: " + e.getMessage());
                } finally {
                    latch.countDown();
                }
            });
        });

        latch.await(30, TimeUnit.SECONDS);
        assertEquals(CONCURRENCY, successCount.get());

        // 验证高额列表数据
        String result = getHighStakes(BET_ID);
        assertEquals(20, result.split(",").length);
    }

    // ---------- 边界测试 ----------
    @Test
    public void testMinAmount() throws Exception {
        String sessionKey = getSession(TEST_CUSTOMER_ID + 3);
        assertEquals(200, postStake(TEST_BET_ID + 4, sessionKey, 1));
    }

    @Test
    public void testMaxAmount() throws Exception {
        String sessionKey = getSession(TEST_CUSTOMER_ID + 4);
        assertEquals(200, postStake(TEST_BET_ID + 5, sessionKey, Integer.MAX_VALUE));
    }

    // ---------- 异常测试 ----------
    @Test
    public void testInvalidSessionKey() throws Exception {
        int statusCode = postStake(TEST_BET_ID + 6, "INVALID_KEY", 3000);
        assertEquals(401, statusCode);
    }

    @Test
    public void testNegativeAmount() throws Exception {
        String sessionKey = getSession(TEST_CUSTOMER_ID + 5);
        HttpURLConnection conn = (HttpURLConnection) new URL(BASE_URL + "/" + (TEST_BET_ID + 7) + "/stake?sessionkey=" + sessionKey).openConnection();
        conn.setRequestMethod("POST");
        conn.setDoOutput(true);
        try (OutputStream os = conn.getOutputStream()) {
            os.write("-500".getBytes());
        }
        assertEquals(400, conn.getResponseCode());
    }

    // ================= 验证工具 =================
    private static void validateSessionKey(String key) {
        assertTrue("会话密钥格式错误", key.matches("^[A-Za-z0-9]{7}$"));
    }
}