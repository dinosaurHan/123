package com.betbrain;

import org.junit.*;
import java.net.*;
import java.io.*;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.*;
import java.util.stream.*;

/**
 * 压力测试
 */
public class PressureTest {

    private static final String BASE_URL = "http://localhost:8001";
    private static final int DURATION_SECONDS = 60;
    private static final int[] CONCURRENCY_LEVELS = {50, 100, 200}; // 不同并发级别

    // 性能统计数据结构
    private static class PerfStats {
        AtomicLong requests = new AtomicLong();
        AtomicLong success = new AtomicLong();
        LongAdder latency = new LongAdder();
        ConcurrentLinkedDeque<Long> latencies = new ConcurrentLinkedDeque<>();
    }

    // 测试模式枚举
    private enum TestMode {
        SESSION_ONLY,
        STAKE_ONLY,
        HIGH_STAKES_ONLY,
        MIXED
    }

    @Test
    public void runAllPressureTests() throws Exception {
        for (int concurrency : CONCURRENCY_LEVELS) {
            testPressure(TestMode.SESSION_ONLY, concurrency);
            testPressure(TestMode.STAKE_ONLY, concurrency);
            testPressure(TestMode.HIGH_STAKES_ONLY, concurrency);
            testPressure(TestMode.MIXED, concurrency);
        }
    }

    private void testPressure(TestMode mode, int concurrency) throws Exception {
        // 初始化统计
        Map<String, PerfStats> stats = new ConcurrentHashMap<>();
        stats.put("session", new PerfStats());
        stats.put("stake", new PerfStats());
        stats.put("highStakes", new PerfStats());

        ExecutorService executor = Executors.newFixedThreadPool(concurrency);
        CountDownLatch latch = new CountDownLatch(concurrency);

        // 测试任务
        Runnable task = () -> {
            try {
                long endTime = System.currentTimeMillis() + DURATION_SECONDS * 1000;
                ThreadLocalRandom rand = ThreadLocalRandom.current();

                while (System.currentTimeMillis() < endTime) {
                    switch (mode) {
                        case SESSION_ONLY:
                            testCreateSession(stats.get("session"), rand);
                            break;
                        case STAKE_ONLY:
                            testStakeOperation(stats.get("stake"), rand);
                            break;
                        case HIGH_STAKES_ONLY:
                            testHighStakes(stats.get("highStakes"), rand);
                            break;
                        case MIXED:
                            // 混合比例：session:30%, stake:50%, highStakes:20%
                            double ratio = rand.nextDouble();
                            if (ratio < 0.3) {
                                testCreateSession(stats.get("session"), rand);
                            } else if (ratio < 0.8) {
                                testStakeOperation(stats.get("stake"), rand);
                            } else {
                                testHighStakes(stats.get("highStakes"), rand);
                            }
                            break;
                    }
                }
            } finally {
                latch.countDown();
            }
        };

        // 启动测试
        for (int i = 0; i < concurrency; i++) {
            executor.submit(task);
        }

        // 等待测试完成
        latch.await(DURATION_SECONDS + 30, TimeUnit.SECONDS);
        executor.shutdownNow();

        // 生成报告
        generateReport(mode, concurrency, stats);
    }

    // ================= 接口测试方法 =================
    private void testCreateSession(PerfStats stats, ThreadLocalRandom rand) {
        long start = System.nanoTime();
        try {
            int customerId = rand.nextInt(1_000_000);
            HttpURLConnection conn = (HttpURLConnection) new URL(BASE_URL + "/" + customerId + "/session").openConnection();
            try (BufferedReader br = new BufferedReader(new InputStreamReader(conn.getInputStream()))) {
                String key = br.readLine();
                if (key != null && key.length() == 7) {
                    stats.success.incrementAndGet();
                }
            }
        } catch (Exception e) {
            // 错误计数
        } finally {
            recordLatency(stats, start);
        }
    }

    private void testStakeOperation(PerfStats stats, ThreadLocalRandom rand) {
        long start = System.nanoTime();
        try {
            int customerId = rand.nextInt(1_000_000);
            String sessionKey = getSession(customerId); // 可能会触发创建会话操作
            int betId = rand.nextInt(1000);
            int amount = rand.nextInt(1, 10_001); // 随机金额1-10000

            HttpURLConnection conn = (HttpURLConnection) new URL(BASE_URL + "/" + betId + "/stake?sessionkey=" + sessionKey)
                    .openConnection();
            conn.setRequestMethod("POST");
            conn.setDoOutput(true);
            try (OutputStream os = conn.getOutputStream()) {
                os.write(Integer.toString(amount).getBytes());
            }

            if (conn.getResponseCode() == 200) {
                stats.success.incrementAndGet();
            }
        } catch (Exception e) {
            // 错误计数
        } finally {
            recordLatency(stats, start);
        }
    }

    private void testHighStakes(PerfStats stats, ThreadLocalRandom rand) {
        long start = System.nanoTime();
        try {
            int betId = rand.nextInt(1000);
            HttpURLConnection conn = (HttpURLConnection) new URL(BASE_URL + "/" + betId + "/highstakes")
                    .openConnection();
            try (BufferedReader br = new BufferedReader(new InputStreamReader(conn.getInputStream()))) {
                String response = br.lines().collect(Collectors.joining());
                if (!response.contains("error")) {
                    stats.success.incrementAndGet();
                }
            }
        } catch (Exception e) {
            // 错误计数
        } finally {
            recordLatency(stats, start);
        }
    }

    // ================= 辅助方法 =================
    private void recordLatency(PerfStats stats, long startTime) {
        long latency = (System.nanoTime() - startTime) / 1_000_000; // 毫秒
        stats.latency.add(latency);
        stats.latencies.add(latency);
        stats.requests.incrementAndGet();
    }

    private void generateReport(TestMode mode, int concurrency, Map<String, PerfStats> stats) {
        try (PrintWriter writer = new PrintWriter(
                String.format("report-%s-%d.txt", mode, concurrency))) {

            writer.println("======= 压力测试报告 =======");
            writer.printf("测试模式: %s%n", mode);
            writer.printf("并发数: %,d%n", concurrency);
            writer.printf("持续时间: %d秒%n", DURATION_SECONDS);
            writer.println("--------------------------");

            stats.forEach((api, s) -> {
                double qps = s.requests.get() / (double)DURATION_SECONDS;
                double avgLatency = s.latency.doubleValue() / s.requests.get();

                writer.printf("[%s] QPS: %.2f, 平均延迟: %.2fms, 成功率: %.2f%%%n",
                        api.toUpperCase(), qps, avgLatency,
                        s.success.get() * 100.0 / s.requests.get());
            });
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private static String getSession(int customerId) throws IOException {
        HttpURLConnection conn = (HttpURLConnection) new URL(BASE_URL + "/" + customerId + "/session").openConnection();
        try (BufferedReader br = new BufferedReader(new InputStreamReader(conn.getInputStream()))) {
            return br.readLine();
        }
    }
}