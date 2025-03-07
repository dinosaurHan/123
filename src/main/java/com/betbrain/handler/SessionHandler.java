package com.betbrain.handler;

import com.betbrain.server.Handler;
import com.betbrain.service.SessionService;
import com.betbrain.util.HttpUtil;
import com.sun.net.httpserver.HttpExchange;

import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Handles session creation and management requests
 */
public class SessionHandler implements Handler {

    private static final Logger logger = Logger.getLogger(SessionHandler.class.getName());
    private final SessionService sessionService = SessionService.getInstance();

    /**
     * Processes session-related HTTP requests
     *
     * @param exchange HTTP exchange containing request/response objects
     */
    @Override
    public void handle(HttpExchange exchange) throws IOException {
        try {
            int customerId = extractCustomerId(exchange);
            String sessionKey = sessionService.getOrCreateSession(customerId);
            HttpUtil.sendResponse(exchange, HttpUtil.HTTP_OK, sessionKey);
        } catch (IllegalArgumentException e) {
            HttpUtil.sendResponse(exchange, HttpUtil.BAD_REQUEST, "Invalid customer ID format");
        } catch (IllegalStateException e) {
            HttpUtil.sendResponse(exchange, HttpUtil.SERVICE_UNAVAILABLE, "Service unavailable");
        } catch (Exception e) {
            logger.log(Level.SEVERE, "Unexpected error handling session request", e);
            HttpUtil.sendResponse(exchange, HttpUtil.SERVICE_UNAVAILABLE, "Internal server error");
        }
    }

    /**
     * Extracts customer ID from request path
     *
     * @param exchange HTTP exchange object
     * @return Validated customer ID
     * @throws IllegalArgumentException for invalid ID formats
     */
    private int extractCustomerId(HttpExchange exchange) throws IllegalArgumentException {
        String path = exchange.getRequestURI().getPath();
        String[] pathSegments = path.split("/");

        if (pathSegments.length < 2) {
            throw new IllegalArgumentException("Missing customer ID in path");
        }

        try {
            int customerId = Integer.parseInt(pathSegments[1]);
            if (customerId < 0) {
                throw new IllegalArgumentException("Customer ID cannot be negative");
            }
            return customerId;
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("Invalid customer ID format", e);
        }
    }


}