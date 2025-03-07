package com.betbrain.util;

import com.sun.net.httpserver.HttpExchange;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;

public class HttpUtil {

    public static final String CONTENT_TYPE = "text/plain";

    public static final int HTTP_OK = 200;
    public static final int BAD_REQUEST = 400;
    public static final int UNAUTHORIZED = 401;
    public static final int NOT_FOUND_CODE = 404;
    public static final int INTERNAL_SERVER_ERROR = 500;
    public static final int SERVICE_UNAVAILABLE = 503;

    public static final String ERROR_MESSAGE_INTERNAL_SERVER_ERROR = "Server is currently unavailable. Please try again later.";

    /**
     * Sends HTTP response with specified status and content
     *
     * @param exchange   HTTP exchange object
     * @param statusCode HTTP response code
     * @param content    Response body content
     */
    public static void sendResponse(HttpExchange exchange, int statusCode, String content) throws IOException {
        exchange.getResponseHeaders().set("Content-Type", CONTENT_TYPE);
        boolean hasContent = content != null && !content.isEmpty();
        if (hasContent) {
            byte[] responseBytes = content.getBytes(StandardCharsets.UTF_8);
            exchange.sendResponseHeaders(statusCode, responseBytes.length);
            try (OutputStream os = exchange.getResponseBody()) {
                os.write(responseBytes);
            }
        } else {
            exchange.sendResponseHeaders(statusCode, -1);
        }
    }
}
