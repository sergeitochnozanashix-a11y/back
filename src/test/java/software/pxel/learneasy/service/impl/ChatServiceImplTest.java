package software.pxel.learneasy.service.impl;

import jakarta.persistence.EntityNotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import software.pxel.learneasy.api.dto.chat.UserChatDto;
import software.pxel.learneasy.api.dto.chat.request.AttachmentRequestDto;
import software.pxel.learneasy.api.dto.chat.request.SendMessageRequest;
import software.pxel.learneasy.api.dto.chat.response.AttachmentResponseDto;
import software.pxel.learneasy.api.dto.chat.response.ChatMessagesResponse;
import software.pxel.learneasy.api.dto.chat.response.ChatResponse;
import software.pxel.learneasy.api.dto.chat.response.MessageResponseDto;
import software.pxel.learneasy.config.security.UserAuthProvider;
import software.pxel.learneasy.exception.RateLimitExceededException;
import software.pxel.learneasy.mapper.ChatMapper;
import software.pxel.learneasy.model.Chat;
import software.pxel.learneasy.model.ChatMessage;
import software.pxel.learneasy.model.MessageAttachment;
import software.pxel.learneasy.model.User;
import software.pxel.learneasy.repository.ChatMessageRepository;
import software.pxel.learneasy.repository.ChatRepository;
import software.pxel.learneasy.repository.ChatRepository.ChatInfoProjection;
import software.pxel.learneasy.service.RateLimiterService;
import software.pxel.learneasy.service.async.AIChatProcessingQueue;
import software.pxel.learneasy.service.async.AudioTranscriptionQueue;
import software.pxel.learneasy.service.async.TitleGenerationQueue;
import software.pxel.learneasy.service.util.AfterCommitExecutor;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.AssertionsForClassTypes.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.*;
import static software.pxel.learneasy.constants.ChatConstants.CHAT_WITH_ID_NOT_FOUND_RESPONSE;

@ExtendWith(MockitoExtension.class)
public class ChatServiceImplTest {

    @Mock
    private ChatRepository chatRepository;

    @Mock
    private UserAuthProvider authProvider;

    @Mock
    private ChatMapper chatMapper;

    @Mock
    private ChatMessageRepository chatMessageRepository;

    @Mock
    private RateLimiterService rateLimiterService;

    @Mock
    private AudioTranscriptionQueue audioTranscriptionQueue;

    @Mock
    private AIChatProcessingQueue aiChatProcessingQueue;

    @Mock
    private TitleGenerationQueue titleGenerationQueue;

    @Mock
    private AfterCommitExecutor afterCommitExecutor;

    @InjectMocks
    private ChatServiceImpl chatService;

    private User testUser;
    private Chat testChat1;

    @BeforeEach
    void setUp() {
        testUser = User.builder()
                .email("test@example.com")
                .build();
        testUser.setId(1L);

        testChat1 = Chat.builder()
                .user(testUser)
                .title("Chat 1")
                .build();
        testChat1.setId(1L);

        when(authProvider.getCurrentUser()).thenReturn(testUser);
    }

    @Test
    @DisplayName("Успешное создание нового чата и возврат его ID")
    void createNewChat_ShouldCreateAndReturnChatId() {
        when(chatRepository.save(any(Chat.class))).thenAnswer(invocation -> {
            Chat savedChat = invocation.getArgument(0);
            savedChat.setId(123L);
            return savedChat;
        });

        Long chatId = chatService.createNewChat();

        assertThat(chatId).isEqualTo(123L);
        verify(chatRepository, times(1)).save(any(Chat.class));
    }

    @Test
    @DisplayName("Получение списка чатов с правильной сортировкой и данными")
    void getListChatsUserSortDate_ShouldReturnSortedChats() {
        // Arrange
        Instant newestTimestamp = Instant.now();
        Instant olderTimestamp = newestTimestamp.minus(1, ChronoUnit.HOURS);

        Chat testChat2 = Chat.builder().title("Chat 2").build();
        testChat2.setId(2L);
        Chat testChat3 = Chat.builder().title("Chat 3").build();
        testChat3.setId(3L);


        ChatInfoProjection projection2 = new ChatInfoProjection(testChat2, newestTimestamp);
        ChatInfoProjection projection1 = new ChatInfoProjection(testChat1, olderTimestamp);
        ChatInfoProjection projection3 = new ChatInfoProjection(testChat3, null);
        List<ChatInfoProjection> projections = Arrays.asList(projection2, projection1, projection3);

        when(chatRepository.findUserChatsWithLatestMessageTimestamp(1L)).thenReturn(projections);

        when(chatMapper.chatInfoProjectionToUserChatDto(projection2))
                .thenReturn(new UserChatDto(2L, "Chat 2", newestTimestamp.toString()));
        when(chatMapper.chatInfoProjectionToUserChatDto(projection1))
                .thenReturn(new UserChatDto(1L, "Chat 1", olderTimestamp.toString()));
        when(chatMapper.chatInfoProjectionToUserChatDto(projection3))
                .thenReturn(new UserChatDto(3L, "Chat 3", null));

        // Act
        ChatResponse result = chatService.getListChatsUserSortDate();

        // Assert
        assertThat(result.userId()).isEqualTo(1L);
        assertThat(result.userChatDtos()).hasSize(3);
        assertThat(result.userChatDtos()).extracting(UserChatDto::chatId).containsExactly(2L, 1L, 3L);
    }

    @Nested
    @DisplayName("Тестирование метода getAllMessageByChatId()")
    class GetAllMessageByChatId {
        @Test
        @DisplayName("Успешное получение сообщений чата")
        void shouldReturnMessagesWhenChatExists() {
            // Arrange
            Long chatId = 1L;

            MessageAttachment attachment1 = MessageAttachment.builder().originalFilename("doc1.pdf").build();
            attachment1.setId(101L);

            ChatMessage message1 = ChatMessage.builder().content("Сообщение с файлом").attachments(List.of(attachment1)).build();
            message1.setId(1L);

            attachment1.setChatMessage(message1);

            ChatMessage message2 = ChatMessage.builder().content("Сообщение без файла").attachments(new ArrayList<>()).build();
            message2.setId(2L);

            List<ChatMessage> dbMessages = List.of(message1, message2);

            AttachmentResponseDto attachmentDto = new AttachmentResponseDto(101L, null, "doc1.pdf", null, null);
            MessageResponseDto messageDto1 = new MessageResponseDto(1L, "Сообщение с файлом", ChatMessage.Role.USER, null, null, List.of(attachmentDto));
            MessageResponseDto messageDto2 = new MessageResponseDto(2L, "Сообщение без файла", ChatMessage.Role.USER, null, null, Collections.emptyList());

            when(chatRepository.findByIdAndUserId(chatId, 1L)).thenReturn(Optional.of(testChat1));
            when(chatMessageRepository.findByChatIdWithAttachments(chatId)).thenReturn(dbMessages);
            when(chatMapper.chatMessageEntityToMessageResponseDto(message1)).thenReturn(messageDto1);
            when(chatMapper.chatMessageEntityToMessageResponseDto(message2)).thenReturn(messageDto2);

            // Act
            ChatMessagesResponse result = chatService.getAllMessageByChatId(chatId);

            // Assert
            assertThat(result).isNotNull();
            assertThat(result.chatId()).isEqualTo(chatId);
            assertThat(result.title()).isEqualTo(testChat1.getTitle());
            assertThat(result.messages()).hasSize(2);
            assertThat(result.messages().getFirst().attachments()).hasSize(1);
            assertThat(result.messages().get(1).attachments()).isEmpty();
            verify(chatMapper, times(2)).chatMessageEntityToMessageResponseDto(any(ChatMessage.class));
        }

        @Test
        @DisplayName("Возврат пустого списка, если чат существует, но в нем нет сообщений")
        void shouldReturnEmptyListWhenChatIsEmpty() {
            Long chatId = 1L;
            when(chatRepository.findByIdAndUserId(chatId, 1L)).thenReturn(Optional.of(testChat1));
            when(chatMessageRepository.findByChatIdWithAttachments(chatId)).thenReturn(Collections.emptyList());

            ChatMessagesResponse result = chatService.getAllMessageByChatId(chatId);

            assertThat(result).isNotNull();
            assertThat(result.messages()).isNotNull().isEmpty();
            verify(chatMapper, never()).chatMessageEntityToMessageResponseDto(any());
        }

        @Test
        @DisplayName("Выброс исключения, если чат не найден или не принадлежит пользователю")
        void shouldThrowExceptionWhenChatNotFound() {
            Long chatId = 99L;
            when(chatRepository.findByIdAndUserId(chatId, 1L)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> chatService.getAllMessageByChatId(chatId))
                    .isInstanceOf(EntityNotFoundException.class)
                    .hasMessageContaining(String.format(CHAT_WITH_ID_NOT_FOUND_RESPONSE, chatId));

            verify(chatMessageRepository, never()).findByChatIdWithAttachments(anyLong());
        }
    }

    @Nested
    @DisplayName("Тестирование метода sendingMessageToChat()")
    class SendingMessageToChat {

        @BeforeEach
        void setupExecutor() {
            lenient().doAnswer(invocation -> {
                invocation.getArgument(0, Runnable.class).run();
                return null;
            }).when(afterCommitExecutor).execute(any(Runnable.class));
        }

        @Test
        @DisplayName("Текстовое сообщение должно отправляться в очередь AI, но не в очередь транскрипции")
        void shouldSubmitToAIQueueForTextMessage() {
            Long chatId = 1L;
            SendMessageRequest request = new SendMessageRequest("Test message", null, null);
            ChatMessage savedMessage = ChatMessage.builder().build();
            savedMessage.setId(100L);

            when(chatRepository.findByIdAndUserId(chatId, 1L)).thenReturn(Optional.of(testChat1));
            when(chatMessageRepository.save(any(ChatMessage.class))).thenReturn(savedMessage);
            when(chatMessageRepository.findByIdWithAttachments(100L)).thenReturn(Optional.of(savedMessage));

            chatService.sendingMessageToChat(chatId, request);

            verify(rateLimiterService).checkUserLimit(1L);
            verify(chatMessageRepository).save(any(ChatMessage.class));
            verify(aiChatProcessingQueue).submit(chatId);
            verify(audioTranscriptionQueue, never()).submit(anyLong());
        }

        @Test
        @DisplayName("Сообщение с audioFileUrl должно отправляться в очередь транскрипции, но не в очередь AI")
        void shouldSubmitToTranscriptionQueueForAudioUrlMessage() {
            Long chatId = 1L;
            String audioUrl = "http://example.com/audio.mp3";
            SendMessageRequest request = new SendMessageRequest("Voice message", audioUrl, null);

            ChatMessage savedMessage = ChatMessage.builder().audioFileUrl(audioUrl).build();
            savedMessage.setId(101L);

            when(chatRepository.findByIdAndUserId(chatId, 1L)).thenReturn(Optional.of(testChat1));
            when(chatMessageRepository.save(any(ChatMessage.class))).thenReturn(savedMessage);
            when(chatMessageRepository.findByIdWithAttachments(101L)).thenReturn(Optional.of(savedMessage));

            chatService.sendingMessageToChat(chatId, request);

            verify(rateLimiterService).checkUserLimit(1L);
            verify(chatMessageRepository).save(any(ChatMessage.class));
            verify(audioTranscriptionQueue).submit(101L);
            verify(aiChatProcessingQueue, never()).submit(anyLong());
        }

        @Test
        @DisplayName("Сообщение с аудио-вложением должно отправляться в очередь транскрипции, но не в очередь AI")
        void shouldSubmitToTranscriptionQueueForAudioAttachmentMessage() {
            Long chatId = 1L;
            AttachmentRequestDto audioAttachmentDto = new AttachmentRequestDto("url", "audio.mp3", "audio/mpeg", 123L);
            SendMessageRequest request = new SendMessageRequest("Check audio", null, List.of(audioAttachmentDto));

            MessageAttachment audioAttachment = MessageAttachment.builder().contentType("audio/mpeg").build();
            ChatMessage savedMessage = ChatMessage.builder().attachments(List.of(audioAttachment)).build();
            savedMessage.setId(102L);
            audioAttachment.setChatMessage(savedMessage);

            when(chatRepository.findByIdAndUserId(chatId, 1L)).thenReturn(Optional.of(testChat1));
            when(chatMessageRepository.save(any(ChatMessage.class))).thenReturn(savedMessage);
            when(chatMessageRepository.findByIdWithAttachments(102L)).thenReturn(Optional.of(savedMessage));

            chatService.sendingMessageToChat(chatId, request);

            verify(rateLimiterService).checkUserLimit(1L);
            verify(chatMessageRepository).save(any(ChatMessage.class));
            verify(audioTranscriptionQueue).submit(102L);
            verify(aiChatProcessingQueue, never()).submit(anyLong());
        }

        @Test
        @DisplayName("Отправка сообщения - превышен лимит запросов")
        void whenRateLimitExceeded_ShouldThrowException() {
            SendMessageRequest request = new SendMessageRequest("Test message", null, null);
            doThrow(new RateLimitExceededException("Rate limit exceeded")).when(rateLimiterService).checkUserLimit(1L);

            assertThatThrownBy(() -> chatService.sendingMessageToChat(1L, request))
                    .isInstanceOf(RateLimitExceededException.class);

            verify(chatRepository, never()).findByIdAndUserId(anyLong(), anyLong());
        }

        @Test
        @DisplayName("Отправка сообщения - чат не найден")
        void whenChatNotFound_ShouldThrowException() {
            Long chatId = 999L;
            SendMessageRequest request = new SendMessageRequest("Test message", null, null);
            when(chatRepository.findByIdAndUserId(chatId, 1L)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> chatService.sendingMessageToChat(chatId, request))
                    .isInstanceOf(EntityNotFoundException.class);

            verify(chatMessageRepository, never()).save(any(ChatMessage.class));
        }
    }
}
