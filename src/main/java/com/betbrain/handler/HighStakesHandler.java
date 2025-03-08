package com.betbrain.handler;

import com.betbrain.server.Handler;
import com.betbrain.service.StakeService;
import com.betbrain.util.HttpUtil;
import com.betbrain.util.ParamUtil;
import com.sun.net.httpserver.HttpExchange;

import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Handles requests for retrieving top 20 highest stakes
 */
public class HighStakesHandler implements Handler {

    private static final Logger logger = Logger.getLogger(HighStakesHandler.class.getName());
    private final StakeService stakeService = StakeService.getInstance();

    /**
     * Processes requests for top stakes data
     * @param exchange HTTP exchange containing request details
     */
    @Override
    public void handle(HttpExchange exchange) throws IOException {
        try {
            int betId = ParamUtil.parseBetIdFromPath(exchange);
            String response = stakeService.getTop20Stakes(betId);
            HttpUtil.sendResponse(exchange, HttpUtil.HTTP_OK, response);
            logger.info(() -> "Successfully returned top stakes for bet ID: " + betId);
        } catch (IllegalArgumentException e) {
            HttpUtil.sendResponse(exchange, HttpUtil.BAD_REQUEST, "Invalid bet ID format");
            logger.warning("Invalid request: " + e.getMessage());
        } catch (Exception e) {
            logger.log(Level.SEVERE, "Error processing request", e);
            HttpUtil.sendResponse(exchange, HttpUtil.INTERNAL_SERVER_ERROR, "Internal server error");
        }
    }


}