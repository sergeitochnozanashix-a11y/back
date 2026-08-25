package software.pxel.learneasy.api.dto.mainpage;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "DTO с информацией о модуле и прогрессе по нему для главной страницы")
public record ModuleInfoDTO(
        @Schema(description = "ID модуля", example = "1")
        long id,
        @Schema(description = "Порядковый номер модуля в курсе", example = "1")
        int sequenceNumber,
        @Schema(description = "Название модуля", example = "Введение в Java")
        String title,
        @Schema(description = "Всего уроков в модуле", example = "5")
        int totalLessons,
        @Schema(description = "Пройдено уроков в модуле", example = "5")
        long completedLessons,
        @Schema(description = "Статус прохождения модуля", example = "COMPLETED")
        String completionStatus
) {
}
