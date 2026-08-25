package software.pxel.learneasy.service.impl;

import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import software.pxel.learneasy.api.dto.chat.UserChatDto;
import software.pxel.learneasy.api.dto.chat.request.SendMessageRequest;
import software.pxel.learneasy.api.dto.chat.response.ChatMessagesResponse;
import software.pxel.learneasy.api.dto.chat.response.ChatResponse;
import software.pxel.learneasy.api.dto.chat.response.MessageResponseDto;
import software.pxel.learneasy.config.security.UserAuthProvider;
import software.pxel.learneasy.mapper.ChatMapper;
import software.pxel.learneasy.model.Chat;
import software.pxel.learneasy.model.ChatMessage;
import software.pxel.learneasy.model.MessageAttachment;
import software.pxel.learneasy.model.User;
import software.pxel.learneasy.repository.ChatMessageRepository;
import software.pxel.learneasy.repository.ChatRepository;
import software.pxel.learneasy.service.ChatService;
import software.pxel.learneasy.service.RateLimiterService;
import software.pxel.learneasy.service.async.AIChatProcessingQueue;
import software.pxel.learneasy.service.async.AudioTranscriptionQueue;
import software.pxel.learneasy.service.async.TitleGenerationQueue;
import software.pxel.learneasy.service.util.AfterCommitExecutor;

import java.util.List;
import java.util.Objects;

import static software.pxel.learneasy.constants.ChatConstants.CHAT_WITH_ID_NOT_FOUND_RESPONSE;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ChatServiceImpl implements ChatService {

    public static final String DEFAULT_TITLE_FOR_VOICE_MESSAGE = "Голосовое сообщение";

    private final ChatMapper chatMapper;
    private final ChatRepository chatRepository;
    private final UserAuthProvider authProvider;
    private final RateLimiterService rateLimiterService;
    private final AfterCommitExecutor afterCommitExecutor;
    private final ChatMessageRepository chatMessageRepository;
    private final AIChatProcessingQueue aiChatProcessingQueue;
    private final AudioTranscriptionQueue audioTranscriptionQueue;
    private final TitleGenerationQueue titleGenerationQueue;


    @Override
    @Transactional
    public Long createNewChat() {
        User user = getCurrentUser();
        Chat createdChat = Chat.builder()
                .user(user)
                .build();

        chatRepository.save(createdChat);
        log.info("Пользователь с ID:{} создал чат под ID:{}", user.getId(), createdChat.getId());
        return createdChat.getId();
    }

    @Override
    public ChatResponse getListChatsUserSortDate() {
        User user = getCurrentUser();
        List<UserChatDto> chats = chatRepository.findUserChatsWithLatestMessageTimestamp(user.getId())
                .stream()
                .map(chatMapper::chatInfoProjectionToUserChatDto)
                .toList();

        log.info("Список из '{}' чатов возвращен пользователю с ID:{}", chats.size(), user.getId());
        return new ChatResponse(user.getId(), chats);
    }

    @Override
    public ChatMessagesResponse getAllMessageByChatId(Long chatId) {
        User user = getCurrentUser();
        Chat chat = findAndVerifyChat(chatId, user.getId());

        List<MessageResponseDto> messages = chatMessageRepository.findByChatIdWithAttachments(chatId)
                .stream()
                .map(chatMapper::chatMessageEntityToMessageResponseDto)
                .toList();

        if (!messages.isEmpty()) {
            log.info("Возвращено {} сообщений из чата ID:{} для пользователя ID:{}", messages.size(), chatId, user.getId());
        }

        return new ChatMessagesResponse(chat.getId(), chat.getTitle(), messages);
    }

    @Override
    @Transactional
    public void sendingMessageToChat(Long chatId, SendMessageRequest request) {
        User user = getCurrentUser();
        rateLimiterService.checkUserLimit(user.getId());

        Chat chat = findAndVerifyChat(chatId, user.getId());

        boolean isFirstMessage = chat.getTitle() == null || chat.getTitle().isEmpty();
        if (isFirstMessage) {
            initializeTemporaryTitle(chat, request);
        }

        ChatMessage savedMessage = saveUserMessage(chat, request);

        afterCommitExecutor.execute(() -> {
            boolean wasSubmittedForTranscription = submitForTranscriptionIfApplicable(savedMessage.getId());
            // Если сообщение НЕ было отправлено на транскрипцию (т.е. это не аудио),
            // то отправляем его сразу на обработку в AI.

            if (isFirstMessage && !wasSubmittedForTranscription) {
                titleGenerationQueue.submit(chat.getId());
            }

            if (!wasSubmittedForTranscription) {
                aiChatProcessingQueue.submit(chat.getId());
            }
        });
    }

    /**
     * Находит и верифицирует, что чат принадлежит пользователю.
     */
    private Chat findAndVerifyChat(Long chatId, Long userId) {
        return chatRepository.findByIdAndUserId(chatId, userId)
                .orElseThrow(() -> new EntityNotFoundException(String.format(
                        CHAT_WITH_ID_NOT_FOUND_RESPONSE, chatId)));
    }

    /**
     * Сохраняет сообщение пользователя.
     */
    private ChatMessage saveUserMessage(Chat chat, SendMessageRequest request) {
        ChatMessage userMessage = ChatMessage.builder()
                .chat(chat)
                .content(request.text())
                .role(ChatMessage.Role.USER)
                .audioFileUrl(request.audioFileUrl())
                .build();

        if (request.attachments() != null && !request.attachments().isEmpty()) {
            request.attachments().stream()
                    .map(dto -> MessageAttachment.builder()
                            .downloadUrl(dto.downloadUrl())
                            .originalFilename(dto.originalFilename())
                            .contentType(dto.contentType())
                            .size(dto.size())
                            .build())
                    .forEach(userMessage::addAttachment);
        }

        ChatMessage savedMessage = chatMessageRepository.save(userMessage);
        log.info("User message with {} attachments saved for chat ID: {}",
                savedMessage.getAttachments().size(), chat.getId());

        return savedMessage;
    }

    private User getCurrentUser() {
        log.debug("Запрос на поиск пользователя из контекста spring");
        User currentUser = authProvider.getCurrentUser();
        log.debug("Пользователь с ID:{} в контексте spring найден", currentUser.getId());
        return currentUser;
    }

    private void initializeTemporaryTitle(Chat chat, SendMessageRequest request) {
        String messageText = request.text();
        if (messageText != null && !messageText.isBlank()) {
            String[] words = messageText.trim().split("\\s+");
            String title = String.join(" ",
                    java.util.Arrays.copyOfRange(words, 0, Math.min(words.length, 4))
            );
            chat.setTitle(title);
            log.info("Установлен временный title для Chat [{}]: '{}'", chat.getId(), title);
        } else if (request.audioFileUrl() != null && !request.audioFileUrl().isBlank()) {
            chat.setTitle(DEFAULT_TITLE_FOR_VOICE_MESSAGE);
            log.info("Установлен временный title по умолчанию для Chat [{}]: '{}'", chat.getId(), DEFAULT_TITLE_FOR_VOICE_MESSAGE);
        } else {
            log.warn("Попытка установить временный title для Chat [{}], но сообщение пустое и нет аудио.", chat.getId());
        }
    }

    /**
     * Проверяет наличие аудиовложения и ставит задачу на транскрипцию.
     *
     * @return true, если задача была поставлена в очередь, иначе false.
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public boolean submitForTranscriptionIfApplicable(Long savedMessageId) {
        final boolean[] submitted = {false};
        chatMessageRepository.findByIdWithAttachments(savedMessageId).ifPresent(message -> {
            boolean hasAudioUrl = message.getAudioFileUrl() != null && !message.getAudioFileUrl().isBlank();
            boolean hasAudioAttachment = message.getAttachments().stream()
                    .anyMatch(att -> Objects.nonNull(att.getContentType()) && att.getContentType().startsWith("audio/"));

            if (hasAudioUrl || hasAudioAttachment) {
                audioTranscriptionQueue.submit(savedMessageId);
                submitted[0] = true;
            }
        });
        return submitted[0];
    }
}
