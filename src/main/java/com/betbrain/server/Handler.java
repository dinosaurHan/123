package com.betbrain.server;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;

import java.io.IOException;

/**
 * 处理类接口，方便路由，解决HttpServer.createContext只能前缀匹配问题
 *
 * @Date 2025.03.03
 * @Author Paul
 */
public interface Handler extends HttpHandler {

    void handle(HttpExchange exchange) throws IOException;
}
