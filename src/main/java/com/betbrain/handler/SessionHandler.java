package com.betbrain.handler;

import com.betbrain.server.Handler;
import com.betbrain.service.SessionService;
import com.betbrain.util.HttpUtil;
import com.betbrain.util.ParamUtil;
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
            int customerId = ParamUtil.extractCustomerId(exchange);
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


}