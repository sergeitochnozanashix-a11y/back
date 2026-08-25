package software.pxel.learneasy.service.async;

import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import software.pxel.learneasy.model.Chat;
import software.pxel.learneasy.model.ChatMessage;
import software.pxel.learneasy.model.MessageAttachment;
import software.pxel.learneasy.repository.ChatMessageRepository;
import software.pxel.learneasy.service.impl.ChatServiceImpl;

import java.util.Optional;

@Slf4j
@Component
@RequiredArgsConstructor
public class TranscriptionPersistenceHelper {

    private final ChatMessageRepository chatMessageRepository;
    private final AIChatProcessingQueue aiChatProcessingQueue;
    private final TitleGenerationQueue titleGenerationQueue;


    public record TranscriptionData(String downloadUrl) {
    }

    /**
     * Загружает данные для транскрипции в read-only транзакции.
     * Приоритет отдается audioFileUrl, затем ищется вложение.
     */
    @Transactional(readOnly = true)
    public TranscriptionData prepareTranscriptionData(Long chatMessageId) {
        ChatMessage message = chatMessageRepository.findByIdWithAttachments(chatMessageId)
                .orElseThrow(() -> new EntityNotFoundException("Message not found: " + chatMessageId));

        if (message.getAudioFileUrl() != null && !message.getAudioFileUrl().isBlank()) {
            return new TranscriptionData(message.getAudioFileUrl());
        }

        return message.getAttachments().stream()
                .filter(this::isAudioAttachment)
                .findFirst()
                .map(audioAttachment -> new TranscriptionData(audioAttachment.getDownloadUrl()))
                .orElse(null);
    }

    /**
     * Сохраняет результат транскрипции и инициирует следующий шаг - обработку AI.
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void saveTranscriptionResult(Long chatMessageId, String transcript) {
        Optional<ChatMessage> messageOptional = chatMessageRepository.findById(chatMessageId);
        if (messageOptional.isEmpty()) {
            log.warn("Could not find message with ID [{}] to save transcription.", chatMessageId);
            return;
        }

        ChatMessage message = messageOptional.get();
        message.setVoiceTranscript(transcript);
        chatMessageRepository.save(message);
        log.info("Successfully transcribed and updated message [{}].", chatMessageId);

        Chat chat = message.getChat();

        if (ChatServiceImpl.DEFAULT_TITLE_FOR_VOICE_MESSAGE.equals(chat.getTitle())) {
            titleGenerationQueue.submit(chat.getId());
        }

        aiChatProcessingQueue.submit(chat.getId());
    }

    private boolean isAudioAttachment(MessageAttachment attachment) {
        if (attachment == null || attachment.getContentType() == null) {
            return false;
        }
        return attachment.getContentType().startsWith("audio/");
    }
}
