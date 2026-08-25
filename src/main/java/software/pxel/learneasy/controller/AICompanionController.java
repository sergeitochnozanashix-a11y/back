package software.pxel.learneasy.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import software.pxel.learneasy.api.dto.chat.request.SendMessageRequest;
import software.pxel.learneasy.api.dto.chat.response.ChatMessagesResponse;
import software.pxel.learneasy.api.dto.chat.response.ChatResponse;
import software.pxel.learneasy.api.dto.chat.response.CreateChatResponse;
import software.pxel.learneasy.controller.api.AICompanionApi;
import software.pxel.learneasy.service.ChatService;

import static software.pxel.learneasy.constants.ApiRoutes.AI_COMPANION_URI;

@Validated
@RestController
@RequiredArgsConstructor
@RequestMapping(AI_COMPANION_URI)
public class AICompanionController implements AICompanionApi {

    private final ChatService chatService;

    @Override
    @PostMapping
    @PreAuthorize("hasAnyAuthority('USER', 'ADMIN')")
    public ResponseEntity<CreateChatResponse> createNewChat() {
        Long chatId = chatService.createNewChat();
        return ResponseEntity.status(HttpStatus.CREATED).body(new CreateChatResponse(chatId));
    }

    @Override
    @GetMapping
    @PreAuthorize("hasAnyAuthority('USER', 'ADMIN')")
    public ResponseEntity<ChatResponse> getListChatsUserSortDate() {
        return ResponseEntity.ok(chatService.getListChatsUserSortDate());
    }

    @Override
    @GetMapping("/{chatId}/messages")
    @PreAuthorize("hasAnyAuthority('USER', 'ADMIN')")
    public ResponseEntity<ChatMessagesResponse> getAllMessageByChatId(@PathVariable("chatId") Long chatId) {
        return ResponseEntity.ok(chatService.getAllMessageByChatId(chatId));
    }

    @Override
    @PostMapping("/{chatId}/messages")
    @PreAuthorize("hasAnyAuthority('USER', 'ADMIN')")
    public ResponseEntity<Void> sendingMessageToChat(
            @PathVariable("chatId") Long chatId,
            @RequestBody @Valid SendMessageRequest request
    ) {
        chatService.sendingMessageToChat(chatId, request);
        return ResponseEntity.accepted().build();
    }
}
