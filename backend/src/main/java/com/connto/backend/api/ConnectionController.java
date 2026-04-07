package com.connto.backend.api;

import com.connto.backend.service.ConnectionService;
import com.connto.backend.service.ConnectionService.ConnectionRequestResponse;
import com.connto.backend.service.ConnectionService.FriendSummary;
import com.connto.backend.web.CurrentUser;
import jakarta.validation.constraints.NotNull;
import java.util.List;
import java.util.UUID;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/connections")
public class ConnectionController {

    private final ConnectionService connectionService;

    public ConnectionController(ConnectionService connectionService) {
        this.connectionService = connectionService;
    }

    @PostMapping("/requests")
    public ConnectionRequestResponse send(@RequestBody SendRequest body) {
        return connectionService.sendRequest(CurrentUser.id(), body.toUserId());
    }

    @GetMapping("/requests/incoming")
    public List<ConnectionRequestResponse> incoming() {
        return connectionService.incomingPending(CurrentUser.id());
    }

    @GetMapping("/requests/outgoing")
    public List<ConnectionRequestResponse> outgoing() {
        return connectionService.outgoingPending(CurrentUser.id());
    }

    @PostMapping("/requests/{id}/accept")
    public void accept(@PathVariable UUID id) {
        connectionService.accept(CurrentUser.id(), id);
    }

    @PostMapping("/requests/{id}/decline")
    public void decline(@PathVariable UUID id) {
        connectionService.decline(CurrentUser.id(), id);
    }

    @GetMapping("/friends")
    public List<FriendSummary> friends() {
        return connectionService.listFriends(CurrentUser.id());
    }

    public record SendRequest(@NotNull UUID toUserId) {}
}
