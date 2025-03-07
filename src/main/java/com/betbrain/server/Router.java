package com.betbrain.server;

import com.betbrain.BettingServer;
import com.betbrain.util.HttpUtil;
import com.sun.net.httpserver.HttpExchange;
import java.io.IOException;
import java.io.OutputStream;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Logger;
import java.util.regex.Pattern;

/**
 * Handles HTTP request routing using regular expression patterns
 */
public class Router implements Handler {
    private static final Logger logger = Logger.getLogger(Router.class.getName());
    private static final String CONTENT_TYPE = "text/plain";

    private final Map<Pattern, Handler> routeMap = new HashMap<>();

    /**
     * Registers a new route pattern with corresponding handler
     * @param regexPattern Regular expression pattern for URL matching
     * @param handler Request handler implementation
     */
    public void addRoute(String regexPattern, Handler handler) {
        routeMap.put(Pattern.compile(regexPattern), handler);
        logger.fine(() -> "Route added: " + regexPattern);
    }

    /**
     * Main request handling method with server status check
     * @param exchange HTTP exchange object
     */
    @Override
    public void handle(HttpExchange exchange) throws IOException {
        if (BettingServer.isShuttingDown()) {
            sendErrorResponse(exchange, HttpUtil.SERVICE_UNAVAILABLE,
                    "Service Unavailable - Server is shutting down");
            return;
        }

        String requestPath = exchange.getRequestURI().getPath();
        Handler matchedHandler = findMatchingHandler(requestPath);

        if (matchedHandler != null) {
            matchedHandler.handle(exchange);
        } else {
            sendErrorResponse(exchange, HttpUtil.NOT_FOUND_CODE,
                    "Not Found - Resource does not exist: " + requestPath);
        }
    }

    /**
     * Finds handler for requested path using pattern matching
     * @param path Request URL path
     * @return Matching handler or null if not found
     */
    private Handler findMatchingHandler(String path) {
        return routeMap.entrySet().stream()
                .filter(entry -> entry.getKey().matcher(path).matches())
                .findFirst()
                .map(Map.Entry::getValue)
                .orElse(null);
    }

    /**
     * Sends standardized error responses
     * @param exchange HTTP exchange object
     * @param statusCode HTTP status code
     * @param message Response message
     */
    private void sendErrorResponse(HttpExchange exchange, int statusCode, String message)
            throws IOException {

        logger.warning(() -> String.format("Error %d: %s", statusCode, message));

        exchange.getResponseHeaders().set("Content-Type", CONTENT_TYPE);
        byte[] response = message.getBytes();

        exchange.sendResponseHeaders(statusCode, response.length);
        try (OutputStream os = exchange.getResponseBody()) {
            os.write(response);
        }
    }
}