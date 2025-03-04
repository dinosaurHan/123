package com.betbrain.handler;

import com.betbrain.server.Handler;
import com.betbrain.service.SessionService;
import com.sun.net.httpserver.HttpExchange;

import java.io.IOException;
import java.io.OutputStream;
import java.util.logging.Logger;

/**
 * 会话服务处理类
 *
 * @Date 2025.03.03
 * @Author Paul
 */
public class SessionHandler implements Handler {

    private static final Logger log = Logger.getLogger("SessionHandler");
    private final SessionService sessionService = SessionService.getInstance();

    @Override
    public void handle(HttpExchange exchange) throws IOException {
        String path = exchange.getRequestURI().getPath();
        String[] parts = path.split("/");
        int customerId = Integer.parseInt(parts[1]);

        // 获取或创建会话
        try {
            String sessionKey = sessionService.getOrCreateSession(customerId);
            exchange.sendResponseHeaders(200, sessionKey.length());
            try (OutputStream os = exchange.getResponseBody()) {
                os.write(sessionKey.getBytes());
            }
        } catch (IllegalStateException e) {
            exchange.sendResponseHeaders(503, -1);
        }
    }
}
