package software.pxel.learneasy.api.dto.lesson;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;

@Builder
@Schema(description = "Модель ответа для получения данных по уроку")
public record LessonWithoutContentList(
        @Schema(description = "Уникальный номер урока", example = "1") Long id,
        @Schema(description = "Название урока", example = "HTML Basics") String title,
        @Schema(description = "Описание урока", example = "Structure of a web page.") String description,
        @Schema(description = "Порядковый номер урока в модуле", example = "1") Integer sequenceOrder
) {
}
