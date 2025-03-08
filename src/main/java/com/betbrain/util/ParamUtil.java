package com.betbrain.util;

import com.sun.net.httpserver.HttpExchange;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;

public class ParamUtil {


    /**
     * Extracts bet ID from request path
     * @param exchange HTTP exchange object
     * @return Validated bet ID
     * @throws IllegalArgumentException for invalid ID formats
     */
    public static int parseBetIdFromPath(HttpExchange exchange) throws IllegalArgumentException {
        String[] pathSegments = exchange.getRequestURI().getPath().split("/");

        if (pathSegments.length < 2) {
            throw new IllegalArgumentException("Missing bet ID in path");
        }

        try {
            return Integer.parseUnsignedInt(pathSegments[1]);
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("Invalid bet ID format", e);
        }
    }

    public static int parseBetIdFromPath(String path) {
        String[] segments = path.split("/");
        if (segments.length < 3) {
            throw new IllegalArgumentException("Invalid path format");
        }

        try {
            return Integer.parseUnsignedInt(segments[1]);
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("Invalid bet ID format");
        }
    }

    /**
     * Extracts customer ID from request path
     *
     * @param exchange HTTP exchange object
     * @return Validated customer ID
     * @throws IllegalArgumentException for invalid ID formats
     */
    public static int extractCustomerId(HttpExchange exchange) throws IllegalArgumentException {
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

    /**
     * Extracts session key from request
     *
     * @param exchange HTTP exchange object
     * @return session key
     * @throws IllegalArgumentException for invalid session key formats
     */
    public static String extractSessionKey(HttpExchange exchange) {
        String query = exchange.getRequestURI().getQuery();
        if (query == null || !query.startsWith("sessionkey=")) {
            throw new IllegalArgumentException("Missing session key");
        }
        return query.substring("sessionkey=".length());
    }

    /**
     * read stake amount from request body
     *
     * @param exchange HTTP exchange object
     * @return stake amount
     * @throws IllegalArgumentException for invalid stake amount
     */
    public static int readStakeAmount(HttpExchange exchange) throws IOException {
        ByteArrayOutputStream buffer = new ByteArrayOutputStream();
        try (InputStream is = exchange.getRequestBody()) {
            byte[] data = new byte[1024];
            int bytesRead;
            while ((bytesRead = is.read(data, 0, data.length)) != -1) {
                buffer.write(data, 0, bytesRead);
            }

            String amountStr = buffer.toString("UTF-8").trim();
            return Integer.parseUnsignedInt(amountStr);
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("Invalid stake amount format");
        }
    }

}
