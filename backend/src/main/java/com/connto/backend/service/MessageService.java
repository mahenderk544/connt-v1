package com.connto.backend.service;

import com.connto.backend.domain.AppUser;
import com.connto.backend.domain.DirectMessage;
import com.connto.backend.repository.AppUserRepository;
import com.connto.backend.repository.DirectMessageRepository;
import com.connto.backend.repository.FriendshipRepository;
import com.connto.backend.web.ApiException;
import jakarta.transaction.Transactional;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

@Service
public class MessageService {

    private final DirectMessageRepository messages;
    private final FriendshipRepository friendships;
    private final AppUserRepository users;

    public MessageService(
            DirectMessageRepository messages,
            FriendshipRepository friendships,
            AppUserRepository users) {
        this.messages = messages;
        this.friendships = friendships;
        this.users = users;
    }

    public List<MessageResponse> thread(UUID me, UUID peer) {
        ensureFriends(me, peer);
        return messages.findThread(me, peer).stream()
                .sorted(Comparator.comparing(DirectMessage::getCreatedAt))
                .map(MessageResponse::from)
                .collect(Collectors.toList());
    }

    @Transactional
    public MessageResponse send(UUID fromId, UUID toId, String body) {
        if (fromId.equals(toId)) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Cannot message yourself");
        }
        ensureFriends(fromId, toId);
        if (body == null || body.isBlank()) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Message body required");
        }
        AppUser from = users.getReferenceById(fromId);
        AppUser to = users.getReferenceById(toId);
        DirectMessage m = new DirectMessage();
        m.setFromUser(from);
        m.setToUser(to);
        m.setBody(body.trim());
        messages.save(m);
        return MessageResponse.from(m);
    }

    private void ensureFriends(UUID a, UUID b) {
        UUID low = a.compareTo(b) < 0 ? a : b;
        UUID high = a.compareTo(b) < 0 ? b : a;
        if (friendships.findByOrderedPair(low, high).isEmpty()) {
            throw new ApiException(HttpStatus.FORBIDDEN, "Not connected with this user");
        }
    }

    public record MessageResponse(
            UUID id, UUID fromUserId, UUID toUserId, String body, String createdAt) {
        static MessageResponse from(DirectMessage m) {
            return new MessageResponse(
                    m.getId(),
                    m.getFromUser().getId(),
                    m.getToUser().getId(),
                    m.getBody(),
                    m.getCreatedAt().toString());
        }
    }
}
