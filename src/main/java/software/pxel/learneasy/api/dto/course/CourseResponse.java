package software.pxel.learneasy.api.dto.course;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Модель ответа для курса")
public record CourseResponse(
        @Schema(description = "Уникальный идентификатор курса", example = "1")
        long id,

        @Schema(description = "Название курса", example = "Основы Java-разработки")
        String title,

        @Schema(description = "Описание курса", example = "Курс для начинающих, охватывающий синтаксис Java, ООП и работу с основными коллекциями.")
        String description
) {
}
