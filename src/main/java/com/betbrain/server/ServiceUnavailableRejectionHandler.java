package com.betbrain.server;

import com.sun.net.httpserver.HttpExchange;

import java.io.IOException;
import java.util.concurrent.RejectedExecutionHandler;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.logging.Logger;

/**
 * 自定义拒绝策略
 *
 * @Date 2025.03.03
 * @Author Paul
 */
public class ServiceUnavailableRejectionHandler implements RejectedExecutionHandler {

    private static final Logger log = Logger.getLogger("ServiceUnavailableRejectionHandler");

    @Override
    public void rejectedExecution(Runnable r, ThreadPoolExecutor executor) {
        if (r instanceof HttpExchange) {
            HttpExchange exchange = (HttpExchange) r;
            try {
                exchange.sendResponseHeaders(503, -1);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
    }
}
