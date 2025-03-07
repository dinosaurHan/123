package com.betbrain.handler;

import com.betbrain.util.HttpUtil;
import com.sun.net.httpserver.HttpExchange;
import java.io.IOException;
import java.io.OutputStream;
import java.util.concurrent.RejectedExecutionHandler;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Custom rejection policy for handling server overload situations
 */
public class ServiceUnavailableRejectionHandler implements RejectedExecutionHandler {

    private static final Logger logger = Logger.getLogger(ServiceUnavailableRejectionHandler.class.getName());

    /**
     * Handles rejected HTTP requests when the server is at capacity
     * @param r The rejected Runnable task
     * @param executor The executing thread pool
     */
    @Override
    public void rejectedExecution(Runnable r, ThreadPoolExecutor executor) {
        if (r instanceof HttpExchange) {
            HttpExchange exchange = (HttpExchange) r;
            sendServiceUnavailableResponse(exchange);
        } else {
            logger.warning("Rejected non-HTTP task: " + r.getClass().getName());
        }
    }

    /**
     * Sends standardized 503 Service Unavailable response
     * @param exchange The HTTP exchange to respond to
     */
    private void sendServiceUnavailableResponse(HttpExchange exchange) {
        try {
            logger.log(Level.WARNING, "Rejecting request due to server overload: {0}",
                    exchange.getRequestURI());

            byte[] response = HttpUtil.ERROR_MESSAGE_INTERNAL_SERVER_ERROR.getBytes();
            exchange.sendResponseHeaders(HttpUtil.INTERNAL_SERVER_ERROR, response.length);

            try (OutputStream os = exchange.getResponseBody()) {
                os.write(response);
            }
        } catch (IOException e) {
            logger.log(Level.SEVERE, "Failed to send rejection response", e);
        } finally {
            exchange.close();
        }
    }
}