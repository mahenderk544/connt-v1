package com.connto.backend.call;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.socket.config.annotation.EnableWebSocket;
import org.springframework.web.socket.config.annotation.WebSocketConfigurer;
import org.springframework.web.socket.config.annotation.WebSocketHandlerRegistry;

@Configuration
@EnableWebSocket
public class CallWebSocketConfig implements WebSocketConfigurer {

    private final CallWebSocketHandler callWebSocketHandler;
    private final CallHandshakeInterceptor callHandshakeInterceptor;

    public CallWebSocketConfig(
            CallWebSocketHandler callWebSocketHandler,
            CallHandshakeInterceptor callHandshakeInterceptor) {
        this.callWebSocketHandler = callWebSocketHandler;
        this.callHandshakeInterceptor = callHandshakeInterceptor;
    }

    @Override
    public void registerWebSocketHandlers(WebSocketHandlerRegistry registry) {
        registry.addHandler(callWebSocketHandler, "/ws/call")
                .addInterceptors(callHandshakeInterceptor)
                .setAllowedOriginPatterns("*");
    }
}
