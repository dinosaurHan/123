package com.betbrain.handler;

import com.betbrain.server.Handler;
import com.betbrain.service.SessionService;
import com.betbrain.service.StakeService;
import com.sun.net.httpserver.HttpExchange;

import java.io.IOException;
import java.io.OutputStream;
import java.util.logging.Logger;

/**
 * 查询top 20 投注记录服务处理类
 *
 * @Date 2025.03.03
 * @Author Paul
 */
public class HighStakesHandler implements Handler {

    private static final Logger log = Logger.getLogger("HighStakesHandler");
    private final StakeService stakeService = new StakeService();

    @Override
    public void handle(HttpExchange exchange) throws IOException {
        String path = exchange.getRequestURI().getPath();
        String[] parts = path.split("/");
        int betId = Integer.parseInt(parts[1]);

        // 获取高额赌注
        String response = stakeService.getTop20Stakes(betId);
        exchange.sendResponseHeaders(200, response.length());
        try (OutputStream os = exchange.getResponseBody()) {
            os.write(response.getBytes());
        }
    }
}