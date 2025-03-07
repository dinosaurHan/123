package com.betbrain;

import com.betbrain.handler.HighStakesHandler;
import com.betbrain.handler.SessionHandler;
import com.betbrain.handler.StakeHandler;
import com.betbrain.server.Router;
import com.betbrain.handler.ServiceUnavailableRejectionHandler;
import com.betbrain.service.SessionService;
import com.sun.net.httpserver.HttpServer;

import java.net.InetSocketAddress;
import java.util.concurrent.*;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Main server class for handling betting operations
 */
public class BettingServer {

    private static final Logger logger = Logger.getLogger(BettingServer.class.getName());
    private static final int PORT = 8001;
    private static volatile boolean isShuttingDown = false;

    private HttpServer server;
    private ThreadPoolExecutor executor;

    /**
     * Initializes and starts the HTTP server
     */
    public void start() throws Exception {
        initializeServer();
        configureRoutes();
        setupThreadPool();
        server.start();
        logger.log(Level.INFO, "Server started on port {0}", PORT);
        registerShutdownHook();
    }

    private void initializeServer() throws Exception {
        server = HttpServer.create(new InetSocketAddress("0.0.0.0", PORT), 0);
        server.setExecutor(executor);
    }

    private void configureRoutes() {
        Router router = new Router();
        router.addRoute("/\\d+/session", new SessionHandler());
        router.addRoute("/\\d+/stake", new StakeHandler());
        router.addRoute("/\\d+/highstakes", new HighStakesHandler());
        server.createContext("/", router);
    }

    private void setupThreadPool() {
        int corePoolSize = Runtime.getRuntime().availableProcessors();
        executor = new ThreadPoolExecutor(
                corePoolSize,
                corePoolSize * 2,
                60, TimeUnit.SECONDS,
                new LinkedBlockingQueue<>(5000),
                new ServiceUnavailableRejectionHandler()
        );
    }

    /**
     * Handles graceful server shutdown with proper resource cleanup
     */
    public void shutdown() {
        if (isShuttingDown) return;
        isShuttingDown = true;

        logger.info("Initiating graceful shutdown...");

        // Step 1: Stop accepting new requests
        server.stop(0);

        // Step 2: Shutdown thread pool
        executor.shutdown();
        try {
            if (!executor.awaitTermination(60, TimeUnit.SECONDS)) {
                logger.warning("Forcing shutdown of unfinished tasks");
                executor.shutdownNow();
            }
        } catch (InterruptedException e) {
            logger.log(Level.SEVERE, "Shutdown interrupted", e);
            executor.shutdownNow();
            Thread.currentThread().interrupt();
        }

        // Step 3: Cleanup services
        SessionService.shutdown();

        logger.info("Server shutdown complete.");
    }

    private void registerShutdownHook() {
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            if (!isShuttingDown) {
                shutdown();
            }
        }));
    }

    public static void main(String[] args) throws Exception {
        new BettingServer().start();
    }

    public static boolean isShuttingDown() {
        return isShuttingDown;
    }
}