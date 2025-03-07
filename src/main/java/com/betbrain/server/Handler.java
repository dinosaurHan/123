package com.betbrain.server;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import java.io.IOException;

/**
 * Enhanced HTTP handler interface for flexible routing solutions.
 */
public interface Handler extends HttpHandler {

    /**
     * Processes an HTTP exchange with enhanced routing capabilities
     * @param exchange The HTTP exchange containing request/response objects
     * @throws IOException If an I/O error occurs during processing
     */
    @Override
    void handle(HttpExchange exchange) throws IOException;
}