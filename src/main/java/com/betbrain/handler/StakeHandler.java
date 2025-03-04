package com.betbrain.handler;

import com.betbrain.server.Handler;
import com.betbrain.service.SessionService;
import com.betbrain.service.StakeService;
import com.sun.net.httpserver.HttpExchange;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.logging.Logger;

/**
 * 投注处理服务类
 *
 * @Date 2025.03.03
 * @Author Paul
 */
public class StakeHandler implements Handler {

    private static final Logger log = Logger.getLogger("StakeHandler");
    private final SessionService sessionService = SessionService.getInstance();
    private final StakeService stakeService = new StakeService();

    @Override
    public void handle(HttpExchange exchange) throws IOException {
        // 记录请求开始
        String path = exchange.getRequestURI().getPath();
        String[] parts = path.split("/");
        if (parts.length < 3) {
            log.warning("Invalid path: " + path);
            exchange.sendResponseHeaders(400, -1); // Unauthorized
            return;
        }
        // 解析投注 ID
        int betId;
        try {
            betId = Integer.parseInt(parts[1]);
        } catch (NumberFormatException e) {
            log.warning("Invalid bet ID: " + parts[1]);
            exchange.sendResponseHeaders(400, -1); // Unauthorized
            return;
        }

        // 提取 sessionKey
        String query = exchange.getRequestURI().getQuery();
        String sessionKey = query.substring("sessionkey=".length());
        // 验证会话
        if (!sessionService.isValidSession(sessionKey)) {
            log.warning("Invalid session key: " + sessionKey);
            exchange.sendResponseHeaders(401, -1); // Unauthorized
            return;
        }

        // 读取请求体
        int stakeAmount;
        try (InputStream is = exchange.getRequestBody();
             ByteArrayOutputStream buffer = new ByteArrayOutputStream()) {
            byte[] data = new byte[1024];
            int bytesRead;
            while ((bytesRead = is.read(data, 0, data.length)) != -1) {
                buffer.write(data, 0, bytesRead);
            }
            buffer.flush();
            stakeAmount = Integer.parseInt(buffer.toString("UTF-8"));
        } catch (NumberFormatException e) {
            log.warning("Invalid stake amount in request body");
            exchange.sendResponseHeaders(400, -1); // Bad Request
            return;
        }

        // 验证会话并获取客户 ID
        int customerId = sessionService.getCustomerIdBySessionKey(sessionKey);
        if (customerId == -1) {
            log.warning("Invalid session key: " + sessionKey);
            exchange.sendResponseHeaders(401, -1);
            return;
        }

        // 存储赌注
        try{
            stakeService.addStake(betId, customerId, stakeAmount);
        }catch (Exception e){
            exchange.sendResponseHeaders(400, -1);
        }
        //log.info("===>Stake added: betId=" + betId + ", customerId=" + customerId + ", amount=" + stakeAmount);
        exchange.sendResponseHeaders(200, -1);
    }
}