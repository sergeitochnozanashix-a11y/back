package software.pxel.learneasy.api.dto.userprogress;

import io.swagger.v3.oas.annotations.media.Schema;

public record LessonProgressDTO(
        @Schema(description = "Уникальный идентификатор урока", example = "101")
        Long id,
        @Schema(description = "Название урока", example = "Основные типы данных")
        String title,
        @Schema(description = "Порядковый номер урока в модуле", example = "1")
        Integer sequenceOrder,
        @Schema(description = "Статус урока", example = "NOT_STARTED")
        String status,

        ProgressDetailsDTO progress
) {
}
