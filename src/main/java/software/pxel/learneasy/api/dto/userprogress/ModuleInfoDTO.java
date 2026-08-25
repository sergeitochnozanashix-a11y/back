package software.pxel.learneasy.api.dto.userprogress;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Общая информация и статистика по модулю")
public record ModuleInfoDTO(
        @Schema(description = "Уникальный идентификатор модуля", example = "1")
        Long id,
        @Schema(description = "Название модуля", example = "Введение в программирование")
        String title,
        @Schema(description = "Описание модуля", example = "Базовые концепции и синтаксис")
        String description,
        @Schema(description = "Общее количество уроков в модуле", example = "5")
        int totalLessons,
        @Schema(description = "Количество завершенных уроков пользователем", example = "3")
        long completedLessons,
        @Schema(description = "Статус прохождения модуля", example = "IN_PROGRESS",
                allowableValues = {"NOT_STARTED", "IN_PROGRESS", "COMPLETED"})
        String completionStatus,
        @Schema(description = "Порядковый номер модуля в курсе", example = "1")
        Integer sequenceOrder
) {
}
