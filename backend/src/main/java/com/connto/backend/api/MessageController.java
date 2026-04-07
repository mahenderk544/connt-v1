package com.connto.backend.api;

import com.connto.backend.service.MessageService;
import com.connto.backend.service.MessageService.MessageResponse;
import com.connto.backend.web.CurrentUser;
import jakarta.validation.constraints.NotBlank;
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
@RequestMapping("/api/v1/messages")
public class MessageController {

    private final MessageService messageService;

    public MessageController(MessageService messageService) {
        this.messageService = messageService;
    }

    @GetMapping("/thread/{peerId}")
    public List<MessageResponse> thread(@PathVariable UUID peerId) {
        return messageService.thread(CurrentUser.id(), peerId);
    }

    @PostMapping
    public MessageResponse send(@RequestBody @jakarta.validation.Valid SendMessageRequest body) {
        return messageService.send(CurrentUser.id(), body.toUserId(), body.body());
    }

    public record SendMessageRequest(@NotNull UUID toUserId, @NotBlank String body) {}
}
