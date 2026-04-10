package com.connto.backend.call;

import java.io.IOException;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;

@Component
public class CallSignalingRegistry {

    private final Map<String, Map<UUID, WebSocketSession>> roomSessions = new ConcurrentHashMap<>();

    private static String roomKey(UUID a, UUID b) {
        UUID low = a.compareTo(b) < 0 ? a : b;
        UUID high = a.compareTo(b) < 0 ? b : a;
        return low + "_" + high;
    }

    /** Register this socket for a 1:1 room with {@code peerId}. Replaces any prior socket for the same user in that room. */
    public void register(UUID userId, UUID peerId, WebSocketSession session) {
        String key = roomKey(userId, peerId);
        roomSessions
                .computeIfAbsent(key, k -> new ConcurrentHashMap<>())
                .put(userId, session);
    }

    public void unregister(UUID userId, UUID peerId, WebSocketSession session) {
        String key = roomKey(userId, peerId);
        Map<UUID, WebSocketSession> room = roomSessions.get(key);
        if (room == null) {
            return;
        }
        room.remove(userId, session);
        if (room.isEmpty()) {
            roomSessions.remove(key);
        }
    }

    public void forward(UUID fromUserId, UUID peerId, String jsonEnvelope) throws IOException {
        String key = roomKey(fromUserId, peerId);
        Map<UUID, WebSocketSession> room = roomSessions.get(key);
        if (room == null) {
            return;
        }
        WebSocketSession peer = room.get(peerId);
        if (peer != null && peer.isOpen()) {
            peer.sendMessage(new TextMessage(jsonEnvelope));
        }
    }
}
