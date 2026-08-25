package software.pxel.learneasy.api.dto.chat.response;

import io.swagger.v3.oas.annotations.media.Schema;
import software.pxel.learneasy.model.ChatMessage;

import java.util.List;

@Schema(description = "Ответ с полной информацией о сообщении в чате")
public record MessageResponseDto(
        @Schema(description = "Уникальный идентификатор сообщения", example = "1")
        Long id,

        @Schema(description = "Текст сообщения", example = "Привет! Как дела?")
        String content,

        @Schema(description = "Роль отправителя (USER или ASSISTANT)", example = "USER")
        ChatMessage.Role role,

        @Schema(description = "Опциональный URL прикрепленного аудиофайла")
        String audioFileUrl,

        @Schema(description = "Опциональная транскрипция аудиофайла")
        String voiceTranscript,

        @Schema(description = "Список прикрепленных файлов")
        List<AttachmentResponseDto> attachments
) {
}
