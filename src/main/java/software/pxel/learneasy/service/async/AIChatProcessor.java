package software.pxel.learneasy.service.async;

import feign.FeignException;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import software.pxel.learneasy.api.dto.chat.AttachmentDto;
import software.pxel.learneasy.exception.AIIntegrationException;
import software.pxel.learneasy.feign.ai.dto.ChatCompletionsRequest;
import software.pxel.learneasy.feign.ai.dto.ChatCompletionsResponse;
import software.pxel.learneasy.feign.ai.dto.ChatMessageDto;
import software.pxel.learneasy.feign.ai.service.AIGatewayService;
import software.pxel.learneasy.model.Chat;
import software.pxel.learneasy.model.ChatMessage;
import software.pxel.learneasy.model.MessageAttachment;
import software.pxel.learneasy.repository.ChatMessageRepository;
import software.pxel.learneasy.repository.ChatRepository;
import software.pxel.learneasy.service.FileParsingService;
import software.pxel.learneasy.util.PromptManager;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CompletableFuture;

import static software.pxel.learneasy.constants.ChatConstants.CHAT_WITH_ID_NOT_FOUND_RESPONSE;
import static software.pxel.learneasy.constants.ChatConstants.MODEL_AI_CHAT;

@Service
@RequiredArgsConstructor
@Slf4j
public class AIChatProcessor {

    private final ChatRepository chatRepository;
    private final ChatMessageRepository chatMessageRepository;
    private final AIGatewayService aiGatewayService;
    private final PromptManager promptManager;
    private final FileParsingService fileParsingService;

    @Transactional
    public void processMessage(Long chatId) {
        try {
            Chat chat = chatRepository.findById(chatId)
                    .orElseThrow(() -> new EntityNotFoundException(String.format(CHAT_WITH_ID_NOT_FOUND_RESPONSE, chatId)));

            ChatCompletionsRequest aiRequest = prepareAIRequest(chat);

            ChatCompletionsResponse aiApiResponse = getAIResponse(aiRequest);

            processAndSaveAIResponse(chat, aiApiResponse);

            log.info("Successfully processed and saved AI response for chat ID: {}", chatId);
        } catch (Exception e) {
            log.error("Failed to process message for chat ID: {}. Reason: {}", chatId, e.getMessage(), e);
        }
    }

    private ChatCompletionsRequest prepareAIRequest(Chat chat) {
        List<ChatMessage> history = chatMessageRepository.findByChatIdWithAttachments(chat.getId());

        List<ChatMessageDto> messagesForAI = new ArrayList<>();
        messagesForAI.add(new ChatMessageDto(ChatMessageDto.Role.SYSTEM, promptManager.getChatSystemPrompt(), Collections.emptyList(), null));

        history.forEach(response -> {
            String contentWithAttachments = buildContentWithAttachments(response);

            if (contentWithAttachments == null || contentWithAttachments.isBlank()) {
                return;
            }

            ChatMessageDto.Role roleForAI = ChatMessageDto.Role.valueOf(response.getRole().name());
            messagesForAI.add(new ChatMessageDto(roleForAI, contentWithAttachments, Collections.emptyList(), null));
        });

        return new ChatCompletionsRequest(MODEL_AI_CHAT, messagesForAI);
    }

    private ChatCompletionsResponse getAIResponse(ChatCompletionsRequest aiRequest) {
        try {
            ChatCompletionsResponse response = aiGatewayService.getChatCompletion(aiRequest);
            if (response == null || response.choices() == null || response.choices().isEmpty() ||
                    response.choices().getFirst().message() == null || response.choices().getFirst().message().content() == null) {
                throw new AIIntegrationException("AI service returned an invalid or empty response.");
            }
            return response;
        } catch (FeignException e) {
            log.error("Error calling AI API: {}", e.contentUTF8(), e);
            throw new AIIntegrationException("Failed to get response from AI service.");
        }
    }

    private void processAndSaveAIResponse(Chat chat, ChatCompletionsResponse aiResponse) {
        String aiContent = aiResponse.choices().getFirst().message().content();

        ChatMessage assistantMessage = ChatMessage.builder()
                .chat(chat)
                .content(aiContent)
                .role(ChatMessage.Role.ASSISTANT)
                .build();

        chatMessageRepository.save(assistantMessage);
    }

    /**
     * Собирает финальный контент сообщения, используя транскрипцию как приоритетный
     * источник текста, и асинхронно парся прикрепленные файлы.
     */
    private String buildContentWithAttachments(ChatMessage dbMessage) {
        StringBuilder combinedContent = new StringBuilder();

        String mainContent = dbMessage.getContent();
        if (mainContent == null || mainContent.isBlank()) {
            mainContent = dbMessage.getVoiceTranscript();
        }

        if (mainContent != null && !mainContent.isBlank()) {
            combinedContent.append(mainContent);
        }

        List<MessageAttachment> attachments = dbMessage.getAttachments();
        if (attachments != null && !attachments.isEmpty()) {

            List<CompletableFuture<String>> parsingFutures = attachments.stream()
                    .map(att -> {
                        AttachmentDto dto = new AttachmentDto(att.getId(), att.getDownloadUrl(), att.getOriginalFilename(), att.getContentType(), att.getSize());
                        return fileParsingService.parseFileAsync(dto)
                                .thenApply(parsedText -> formatParsedContent(att.getOriginalFilename(), parsedText));
                    })
                    .toList();

            CompletableFuture.allOf(parsingFutures.toArray(new CompletableFuture[0])).join();

            parsingFutures.forEach(future -> combinedContent.append(future.join()));
        }

        return combinedContent.toString();
    }

    /**
     * Форматирует распарсенный текст файла для добавления в контекст.
     */
    private String formatParsedContent(String filename, String parsedContent) {
        return "\n\n--- [Attachment: " + filename + "] ---\n" +
                parsedContent +
                "\n-------------------------------------\n";
    }
}
