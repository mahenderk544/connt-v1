package com.connto.backend.service;

import com.connto.backend.domain.AppUser;
import com.connto.backend.domain.ConnectionRequest;
import com.connto.backend.domain.ConnectionRequestStatus;
import com.connto.backend.domain.Friendship;
import com.connto.backend.domain.FriendshipId;
import com.connto.backend.repository.AppUserRepository;
import com.connto.backend.repository.ConnectionRequestRepository;
import com.connto.backend.repository.FriendshipRepository;
import com.connto.backend.repository.ProfileRepository;
import com.connto.backend.web.ApiException;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ConnectionService {

    private final AppUserRepository users;
    private final ConnectionRequestRepository requests;
    private final FriendshipRepository friendships;
    private final ProfileRepository profiles;

    public ConnectionService(
            AppUserRepository users,
            ConnectionRequestRepository requests,
            FriendshipRepository friendships,
            ProfileRepository profiles) {
        this.users = users;
        this.requests = requests;
        this.friendships = friendships;
        this.profiles = profiles;
    }

    @Transactional
    public ConnectionRequestResponse sendRequest(UUID fromId, UUID toId) {
        if (fromId.equals(toId)) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Cannot connect to yourself");
        }
        AppUser to =
                users.findById(toId)
                        .orElseThrow(
                                () -> new ApiException(HttpStatus.NOT_FOUND, "User not found"));
        AppUser from = users.getReferenceById(fromId);

        UUID low = fromId.compareTo(toId) < 0 ? fromId : toId;
        UUID high = fromId.compareTo(toId) < 0 ? toId : fromId;
        if (friendships.findByOrderedPair(low, high).isPresent()) {
            throw new ApiException(HttpStatus.CONFLICT, "Already connected");
        }

        var existing = requests.findByFromUserIdAndToUserId(fromId, toId);
        if (existing.isPresent()) {
            ConnectionRequest r = existing.get();
            if (r.getStatus() == ConnectionRequestStatus.PENDING) {
                throw new ApiException(HttpStatus.CONFLICT, "Request already pending");
            }
            if (r.getStatus() == ConnectionRequestStatus.ACCEPTED) {
                throw new ApiException(HttpStatus.CONFLICT, "Already connected");
            }
        }

        var reverse = requests.findByFromUserIdAndToUserId(toId, fromId);
        if (reverse.isPresent() && reverse.get().getStatus() == ConnectionRequestStatus.PENDING) {
            throw new ApiException(
                    HttpStatus.CONFLICT, "They already sent you a request — accept it instead");
        }

        ConnectionRequest req = new ConnectionRequest();
        req.setFromUser(from);
        req.setToUser(to);
        req.setStatus(ConnectionRequestStatus.PENDING);
        requests.save(req);
        return mapConnectionRequest(req);
    }

    @Transactional(readOnly = true)
    public List<ConnectionRequestResponse> incomingPending(UUID userId) {
        return requests.findByToUserIdAndStatusOrderByCreatedAtDesc(
                        userId, ConnectionRequestStatus.PENDING)
                .stream()
                .map(this::mapConnectionRequest)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<ConnectionRequestResponse> outgoingPending(UUID userId) {
        return requests.findByFromUserIdAndStatusOrderByCreatedAtDesc(
                        userId, ConnectionRequestStatus.PENDING)
                .stream()
                .map(this::mapConnectionRequest)
                .collect(Collectors.toList());
    }

    private ConnectionRequestResponse mapConnectionRequest(ConnectionRequest r) {
        String fromName =
                profiles
                        .findByUserId(r.getFromUser().getId())
                        .map(p -> p.getDisplayName())
                        .orElse("User");
        return new ConnectionRequestResponse(
                r.getId(),
                r.getFromUser().getId(),
                r.getToUser().getId(),
                r.getStatus().name(),
                fromName);
    }

    @Transactional
    public void accept(UUID currentUserId, UUID requestId) {
        ConnectionRequest req =
                requests.findById(requestId)
                        .orElseThrow(
                                () ->
                                        new ApiException(
                                                HttpStatus.NOT_FOUND, "Request not found"));
        if (!req.getToUser().getId().equals(currentUserId)) {
            throw new ApiException(HttpStatus.FORBIDDEN, "Not your request to accept");
        }
        if (req.getStatus() != ConnectionRequestStatus.PENDING) {
            throw new ApiException(HttpStatus.CONFLICT, "Request is not pending");
        }
        req.setStatus(ConnectionRequestStatus.ACCEPTED);
        UUID a = req.getFromUser().getId();
        UUID b = req.getToUser().getId();
        UUID low = a.compareTo(b) < 0 ? a : b;
        UUID high = a.compareTo(b) < 0 ? b : a;
        Friendship f = new Friendship();
        f.setId(new FriendshipId(low, high));
        friendships.save(f);
    }

    @Transactional
    public void decline(UUID currentUserId, UUID requestId) {
        ConnectionRequest req =
                requests.findById(requestId)
                        .orElseThrow(
                                () ->
                                        new ApiException(
                                                HttpStatus.NOT_FOUND, "Request not found"));
        if (!req.getToUser().getId().equals(currentUserId)) {
            throw new ApiException(HttpStatus.FORBIDDEN, "Not your request to decline");
        }
        if (req.getStatus() != ConnectionRequestStatus.PENDING) {
            throw new ApiException(HttpStatus.CONFLICT, "Request is not pending");
        }
        req.setStatus(ConnectionRequestStatus.DECLINED);
    }

    @Transactional(readOnly = true)
    public List<FriendSummary> listFriends(UUID userId) {
        return friendships.findAllForUser(userId).stream()
                .map(
                        f -> {
                            UUID peer =
                                    f.getId().getUserLow().equals(userId)
                                            ? f.getId().getUserHigh()
                                            : f.getId().getUserLow();
                            String name =
                                    profiles.findByUserId(peer)
                                            .map(p -> p.getDisplayName())
                                            .orElse("User");
                            return new FriendSummary(peer, name);
                        })
                .sorted(Comparator.comparing(FriendSummary::displayName))
                .collect(Collectors.toList());
    }

    public record ConnectionRequestResponse(
            UUID id, UUID fromUserId, UUID toUserId, String status, String fromDisplayName) {}

    public record FriendSummary(UUID userId, String displayName) {}
}
