package software.pxel.learneasy.api.dto.lesson;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;

@Schema(description = "Запрос на обновление контента урока в формате Markdown")
public record LessonMarkdownContentUpdateRequest(
        @Schema(description = "Новое содержимое урока в формате Markdown")
        @NotNull(message = "Контент не может быть null")
        String content
) {
}
