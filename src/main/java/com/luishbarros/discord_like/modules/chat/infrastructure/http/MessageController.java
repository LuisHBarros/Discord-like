package com.luishbarros.discord_like.modules.chat.infrastructure.http;

import com.luishbarros.discord_like.modules.chat.application.dto.MessageResponse;
import com.luishbarros.discord_like.modules.chat.application.dto.SendMessageRequest;
import com.luishbarros.discord_like.modules.chat.application.dto.UpdateMessageRequest;
import com.luishbarros.discord_like.modules.chat.application.service.MessageService;
import com.luishbarros.discord_like.modules.auth.infrastructure.security.AuthenticatedUser;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.util.List;

@RestController
@RequestMapping("/rooms/{roomId}/messages")
public class MessageController {

    private final MessageService messageService;

    public MessageController(MessageService messageService) {
        this.messageService = messageService;
    }

    @PostMapping
    public ResponseEntity<MessageResponse> sendMessage(
            @AuthenticationPrincipal AuthenticatedUser principal,
            @PathVariable Long roomId,
            @Valid @RequestBody SendMessageRequest request
    ) {
        MessageResponse response = MessageResponse.fromMessage(
                messageService.createMessage(principal.getUserId(), roomId, request.content(), Instant.now())
        );
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping
    public ResponseEntity<List<MessageResponse>> getMessages(
            @AuthenticationPrincipal AuthenticatedUser principal,
            @PathVariable Long roomId,
            @RequestParam(defaultValue = "50") int limit,
            @RequestParam(defaultValue = "0") int offset
    ) {
        List<MessageResponse> response = messageService
                .findByRoomId(roomId, principal.getUserId(), limit, offset)
                .stream()
                .map(MessageResponse::fromMessage)
                .toList();
        return ResponseEntity.ok(response);
    }

    @GetMapping("/before/{messageId}")
    public ResponseEntity<List<MessageResponse>> getMessagesBefore(
            @AuthenticationPrincipal AuthenticatedUser principal,
            @PathVariable Long roomId,
            @PathVariable Long messageId,
            @RequestParam(defaultValue = "50") int limit
    ) {
        List<MessageResponse> response = messageService
                .findByRoomIdBefore(roomId, principal.getUserId(), messageId, limit)
                .stream()
                .map(MessageResponse::fromMessage)
                .toList();
        return ResponseEntity.ok(response);
    }

    @PatchMapping("/{messageId}")
    public ResponseEntity<MessageResponse> updateMessage(
            @AuthenticationPrincipal AuthenticatedUser principal,
            @PathVariable Long roomId,
            @PathVariable Long messageId,
            @Valid @RequestBody UpdateMessageRequest request
    ) {
        MessageResponse response = MessageResponse.fromMessage(
                messageService.updateMessage(principal.getUserId(), roomId, messageId, request.content(), Instant.now())
        );
        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/{messageId}")
    public ResponseEntity<Void> deleteMessage(
            @AuthenticationPrincipal AuthenticatedUser principal,
            @PathVariable Long roomId,
            @PathVariable Long messageId
    ) {
        messageService.deleteMessage(principal.getUserId(), roomId, messageId);
        return ResponseEntity.noContent().build();
    }
}