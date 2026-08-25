package software.pxel.learneasy.service.async;

import feign.FeignException;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import software.pxel.learneasy.exception.AIIntegrationException;
import software.pxel.learneasy.feign.ai.dto.ChatCompletionsRequest;
import software.pxel.learneasy.feign.ai.dto.ChatCompletionsResponse;
import software.pxel.learneasy.feign.ai.dto.ChatMessageDto;
import software.pxel.learneasy.feign.ai.service.AIGatewayService;
import software.pxel.learneasy.model.Chat;
import software.pxel.learneasy.model.ChatMessage;
import software.pxel.learneasy.repository.ChatMessageRepository;
import software.pxel.learneasy.repository.ChatRepository;
import software.pxel.learneasy.service.impl.ChatServiceImpl;
import software.pxel.learneasy.util.PromptManager;

import java.util.Collections;
import java.util.List;

import static software.pxel.learneasy.constants.ChatConstants.CHAT_WITH_ID_NOT_FOUND_RESPONSE;
import static software.pxel.learneasy.constants.ChatConstants.MODEL_AI_CHAT;

@Service
@RequiredArgsConstructor
@Slf4j
public class TitleGeneratorProcessor {

    private final ChatRepository chatRepository;
    private final ChatMessageRepository chatMessageRepository;
    private final AIGatewayService aiGatewayService;
    private final PromptManager promptManager;

    @Transactional
    public void processTitleGeneration(Long chatId) {
        try {
            Chat chat = chatRepository.findById(chatId)
                    .orElseThrow(() -> new EntityNotFoundException(String.format(CHAT_WITH_ID_NOT_FOUND_RESPONSE, chatId)));

            if (!isTemporaryTitle(chat.getTitle())) {
                log.info("Chat [{}] already has a permanent title: '{}'. Skipping generation.", chatId, chat.getTitle());
                return;
            }

            ChatMessage firstMessage = chatMessageRepository.findFirstByChatIdOrderByCreatedAtAsc(chatId)
                    .orElse(null);

            if (firstMessage == null) {
                log.warn("Cannot generate title for chat [{}] because it has no messages.", chatId);
                return;
            }

            String contentForSummary = firstMessage.getVoiceTranscript() != null && !firstMessage.getVoiceTranscript().isBlank()
                    ? firstMessage.getVoiceTranscript()
                    : firstMessage.getContent();

            if (contentForSummary == null || contentForSummary.isBlank()) {
                log.warn("Cannot generate title for chat [{}] as the first message content is empty.", chatId);
                return;
            }

            ChatCompletionsRequest aiRequest = prepareAIRequest(contentForSummary);
            ChatCompletionsResponse aiResponse = getAIResponse(aiRequest);
            String newTitle = aiResponse.choices().getFirst().message().content().replace("\"", "");

            chat.setTitle(newTitle);
            chatRepository.save(chat);

            log.info("Successfully generated and set new title for chat [{}]: '{}'", chatId, newTitle);

        } catch (Exception e) {
            log.error("Failed to generate title for chat ID: {}. Reason: {}", chatId, e.getMessage(), e);
        }
    }

    private boolean isTemporaryTitle(String title) {
        if (title == null) return false;
        if (ChatServiceImpl.DEFAULT_TITLE_FOR_VOICE_MESSAGE.equals(title)) {
            return true;
        }
        return !title.matches(".*[.!?]$");
    }


    private ChatCompletionsRequest prepareAIRequest(String content) {
        List<ChatMessageDto> messages = List.of(
                new ChatMessageDto(ChatMessageDto.Role.SYSTEM, promptManager.getChatTitleSystemPrompt(), Collections.emptyList(), null),
                new ChatMessageDto(ChatMessageDto.Role.USER, content, Collections.emptyList(), null)
        );
        return new ChatCompletionsRequest(MODEL_AI_CHAT, messages);
    }

    private ChatCompletionsResponse getAIResponse(ChatCompletionsRequest aiRequest) {
        try {
            ChatCompletionsResponse response = aiGatewayService.getChatCompletion(aiRequest);
            if (response == null || response.choices() == null || response.choices().isEmpty() ||
                    response.choices().getFirst().message() == null || response.choices().getFirst().message().content() == null) {
                throw new AIIntegrationException("AI service returned an invalid or empty response for title generation.");
            }
            return response;
        } catch (FeignException e) {
            log.error("Error calling AI API for title generation: {}", e.contentUTF8(), e);
            throw new AIIntegrationException("Failed to get response from AI service for title generation.");
        }
    }
}
