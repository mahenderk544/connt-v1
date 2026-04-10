package com.connto.backend.call;

import com.connto.backend.service.ConnectionService;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.util.UUID;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;

@Component
public class CallWebSocketHandler extends TextWebSocketHandler {

    private static final String ATTR_PEER = "callPeerUserId";

    private final ObjectMapper objectMapper;
    private final ConnectionService connections;
    private final CallSignalingRegistry registry;

    public CallWebSocketHandler(
            ObjectMapper objectMapper,
            ConnectionService connections,
            CallSignalingRegistry registry) {
        this.objectMapper = objectMapper;
        this.connections = connections;
        this.registry = registry;
    }

    @Override
    public void afterConnectionEstablished(WebSocketSession session) {
        // wait for join frame
    }

    @Override
    protected void handleTextMessage(WebSocketSession session, TextMessage message) throws Exception {
        UUID userId = (UUID) session.getAttributes().get(CallHandshakeInterceptor.ATTR_USER_ID);
        if (userId == null) {
            session.close(CloseStatus.NOT_ACCEPTABLE);
            return;
        }

        JsonNode root = objectMapper.readTree(message.getPayload());
        UUID peerId = (UUID) session.getAttributes().get(ATTR_PEER);

        if (peerId == null) {
            if (!"join".equals(text(root, "type"))) {
                session.close(CloseStatus.BAD_DATA.withReason("Send join first"));
                return;
            }
            var peerNode = root.get("peerUserId");
            if (peerNode == null || !peerNode.isTextual()) {
                session.close(CloseStatus.BAD_DATA.withReason("peerUserId required"));
                return;
            }
            peerId = UUID.fromString(peerNode.asText());
            if (userId.equals(peerId)) {
                session.close(CloseStatus.BAD_DATA.withReason("Invalid peer"));
                return;
            }
            if (!connections.areConnected(userId, peerId)) {
                session.close(CloseStatus.POLICY_VIOLATION.withReason("Not connected as friends"));
                return;
            }
            session.getAttributes().put(ATTR_PEER, peerId);
            registry.register(userId, peerId, session);
            return;
        }

        ObjectNode envelope = objectMapper.createObjectNode();
        envelope.put("fromUserId", userId.toString());
        envelope.set("signal", root);
        registry.forward(userId, peerId, objectMapper.writeValueAsString(envelope));
    }

    private static String text(JsonNode n, String field) {
        JsonNode v = n.get(field);
        return v != null && v.isTextual() ? v.asText() : null;
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) {
        UUID userId = (UUID) session.getAttributes().get(CallHandshakeInterceptor.ATTR_USER_ID);
        UUID peerId = (UUID) session.getAttributes().get(ATTR_PEER);
        if (userId != null && peerId != null) {
            registry.unregister(userId, peerId, session);
        }
    }
}
