package software.pxel.learneasy.api.dto.chat;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(
        description = "DTO с краткой информацией о чате пользователя"
)
public record UserChatDto(
        @Schema(
                description = "ID чата",
                example = "1"
        )
        Long chatId,

        @Schema(
                description = "Заголовок чата",
                example = "Обсуждение проекта"
        )
        String title,

        @Schema(
                description = "Временная метка последнего сообщения в чате в формате ISO-8601. " +
                        "Может быть null, если в чате нет сообщений.",
                example = "2025-10-09T17:28:00.123Z"
        )
        String updatedAt
) {
}
