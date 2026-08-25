package software.pxel.learneasy.api.dto.chat.response;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;

@Schema(description = "Ответ с полной информацией о чате, включая его заголовок и все сообщения")
public record ChatMessagesResponse(
        @Schema(description = "ID чата", example = "1")
        Long chatId,

        @Schema(description = "Заголовок чата", example = "Обсуждение проекта")
        String title,

        @Schema(description = "Список всех сообщений в чате, отсортированный по времени создания")
        List<MessageResponseDto> messages
) {
}
