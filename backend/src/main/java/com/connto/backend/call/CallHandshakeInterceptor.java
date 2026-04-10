package com.connto.backend.call;

import com.connto.backend.security.JwtService;
import java.util.Map;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.WebSocketHandler;
import org.springframework.web.socket.server.HandshakeInterceptor;

@Component
public class CallHandshakeInterceptor implements HandshakeInterceptor {

    public static final String ATTR_USER_ID = "callUserId";

    private final JwtService jwtService;

    public CallHandshakeInterceptor(JwtService jwtService) {
        this.jwtService = jwtService;
    }

    @Override
    public boolean beforeHandshake(
            ServerHttpRequest request,
            ServerHttpResponse response,
            WebSocketHandler wsHandler,
            Map<String, Object> attributes) {
        String query = request.getURI().getQuery();
        String token = extractToken(query);
        if (token == null || token.isBlank()) {
            response.setStatusCode(HttpStatus.UNAUTHORIZED);
            return false;
        }
        return jwtService
                .parseUserId(token)
                .map(
                        id -> {
                            attributes.put(ATTR_USER_ID, id);
                            return true;
                        })
                .orElseGet(
                        () -> {
                            response.setStatusCode(HttpStatus.UNAUTHORIZED);
                            return false;
                        });
    }

    private static String extractToken(String query) {
        if (query == null || query.isEmpty()) {
            return null;
        }
        for (String part : query.split("&")) {
            if (part.startsWith("token=")) {
                return java.net.URLDecoder.decode(part.substring(6), java.nio.charset.StandardCharsets.UTF_8);
            }
        }
        return null;
    }

    @Override
    public void afterHandshake(
            ServerHttpRequest request,
            ServerHttpResponse response,
            WebSocketHandler wsHandler,
            Exception exception) {}
}
