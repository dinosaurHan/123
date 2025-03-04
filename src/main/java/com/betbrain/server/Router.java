package com.betbrain.server;

import com.betbrain.BettingServer;
import com.sun.net.httpserver.HttpExchange;

import java.io.IOException;
import java.io.OutputStream;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Logger;
import java.util.regex.Pattern;

/**
 * URL路由处理
 *
 * @Date 2025.03.03
 * @Author Paul
 */
public class Router implements Handler {

    private static final Logger log = Logger.getLogger("Router");
    private final Map<Pattern, Handler> routes = new HashMap<>();

    public void addRoute(String pathPattern, Handler handler) {
        routes.put(Pattern.compile(pathPattern), handler);
    }

    @Override
    public void handle(HttpExchange exchange) throws IOException {
        if (BettingServer.isShuttingDown()) {
            sendErrorResponse(exchange, 503, "503 Service Unavailable - Server is shutting down.");
            return;
        }

        String path = exchange.getRequestURI().getPath();

        for (Map.Entry<Pattern, Handler> entry : routes.entrySet()) {
            if (entry.getKey().matcher(path).matches()) {
                entry.getValue().handle(exchange);
                return;
            }
        }

        // 没有匹配的路由
        sendErrorResponse(exchange, 404, "404 Not Found - The requested resource does not exist.");
    }

    private void sendErrorResponse(HttpExchange exchange, int statusCode, String message) throws IOException {
        exchange.sendResponseHeaders(statusCode, message.length());
        try (OutputStream os = exchange.getResponseBody()) {
            os.write(message.getBytes());
        }
    }
}
