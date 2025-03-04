package com.betbrain;

import com.betbrain.handler.HighStakesHandler;
import com.betbrain.handler.SessionHandler;
import com.betbrain.handler.StakeHandler;
import com.betbrain.server.Router;
import com.betbrain.server.ServiceUnavailableRejectionHandler;
import com.betbrain.service.SessionService;
import com.sun.net.httpserver.HttpServer;

import java.net.InetSocketAddress;
import java.util.concurrent.*;
import java.util.logging.Level;
import java.util.logging.Logger;


/**
 * 服务启动类
 *
 * @Date 2025.03.03
 * @Author Paul
 */
public class BettingServer {

    private static final Logger log = Logger.getLogger("BettingServer");
    private static final int PORT = 8001;
    private static volatile boolean isShuttingDown = false; // 是否正在关闭

    public static void main(String[] args) throws Exception {
        HttpServer server = HttpServer.create(new InetSocketAddress("0.0.0.0", PORT), 0);

        // 配置路由
        Router router = new Router();
        router.addRoute("/\\d+/session", new SessionHandler()); // 匹配 /1234/session
        router.addRoute("/\\d+/stake", new StakeHandler());     // 匹配 /5678/stake
        router.addRoute("/\\d+/highstakes", new HighStakesHandler()); // 匹配 /888/highstakes

        server.createContext("/", router); // 绑定到根路径

        // 创建线程池
        int coreCount = Runtime.getRuntime().availableProcessors();
        int maxPoolSize = coreCount * 2;
        int queueCapacity = 5000;
        ThreadPoolExecutor executor = new ThreadPoolExecutor(
                coreCount + 1,
                maxPoolSize + 3,
                60, TimeUnit.SECONDS,
                new LinkedBlockingQueue<>(queueCapacity),
                new ServiceUnavailableRejectionHandler() // 自定义拒绝策略
        );

        server.setExecutor(executor);
        server.start();
        System.out.println("Server started on port " + PORT);

        // 服务器状态监控
        serverStatic(executor, coreCount);

        // 优雅停机
        gracefulShutdown(server,executor);

    }

    /**
     * 线程池&服务器状态监控
     * @param executor
     * @param coreCount
     */
    private static void serverStatic(ThreadPoolExecutor executor, int coreCount) {
        // 线程池监控
        ScheduledExecutorService threadPoolStatic = Executors.newScheduledThreadPool(1);
        threadPoolStatic.scheduleAtFixedRate(() -> {
            log.log(Level.INFO,
                    "ThreadPool Status [active={0}, completed={1}, totalTasks={2}, pending={3}, maxPool={4}, currentPool={5}, isShutdown={6}, queueSize={7},coreCount={8}]",
                    new Object[]{
                            executor.getActiveCount(),
                            executor.getCompletedTaskCount(),
                            executor.getTaskCount(),
                            executor.getTaskCount() - executor.getCompletedTaskCount(),
                            executor.getLargestPoolSize(),
                            executor.getPoolSize(),
                            executor.isShutdown(),
                            executor.getQueue().size(),
                            coreCount
                    }
            );

            // 在BettingServer类中添加监控线程
            Runtime runtime = Runtime.getRuntime();
            System.out.printf("[监控] 内存使用: %,dMB/%,dMB | 线程数: %d%n",
                    (runtime.totalMemory() - runtime.freeMemory()) / 1024 / 1024,
                    runtime.maxMemory() / 1024 / 1024,
                    Thread.activeCount());
        }, 1, 30, TimeUnit.SECONDS);
    }

    /**
     * 停机优化
     * @param server
     * @param executor
     */
    public static void gracefulShutdown(HttpServer server,ThreadPoolExecutor executor) {
        // 优雅停机
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            System.out.println("Shutting down gracefully...");
            isShuttingDown = true; // 标记为正在关闭

            // 停止接受新请求
            server.stop(0);

            // 关闭线程池
            executor.shutdown();
            try {
                if (!executor.awaitTermination(60, TimeUnit.SECONDS)) {
                    executor.shutdownNow();
                }
            } catch (InterruptedException e) {
                executor.shutdownNow();
            }

            // 关闭会话服务和投注服务
            SessionService.shutdown();

            System.out.println("Server has been gracefully shut down.");
        }));
    }

    public static boolean isShuttingDown() {
        return isShuttingDown;
    }

}
