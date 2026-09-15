package software.pxel.learneasy.api.dto.mainpage;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * Имя схемы задано явно, потому что в проекте есть второй класс с тем же
 * простым именем - {@code api.dto.userprogress.ModuleInfoDTO}. springdoc
 * именует схемы по простому имени класса, поэтому они схлопывались в одну:
 * в спеке оставалась версия из userprogress, а main-page отдавал эту.
 * Клиент читал спеку и получал описание чужого DTO.
 */
@Schema(name = "MainPageModuleInfoDTO",
        description = "DTO с информацией о модуле и прогрессе по нему для главной страницы")
public record ModuleInfoDTO(
        @Schema(description = "ID модуля", example = "1")
        long id,
        @Schema(description = "Порядковый номер модуля в курсе", example = "1")
        Integer sequenceOrder,
        @Schema(description = "Название модуля", example = "Введение в Java")
        String title,
        @Schema(description = "Описание модуля", example = "Базовые концепции и синтаксис", nullable = true)
        String description,
        @Schema(description = "Всего уроков в модуле", example = "5")
        int totalLessons,
        @Schema(description = "Пройдено уроков в модуле", example = "5")
        long completedLessons,
        @Schema(description = "Статус прохождения модуля", example = "COMPLETED",
                allowableValues = {"NOT_STARTED", "IN_PROGRESS", "COMPLETED"})
        String completionStatus
) {
}
