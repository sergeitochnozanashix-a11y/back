package software.pxel.learneasy.api.dto.mainpage;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "DTO с информацией о прогрессе по курсу для главной страницы")
public record CourseInfoDTO(
        @Schema(description = "ID курса", example = "1")
        long id,
        @Schema(description = "Название курса", example = "Основы Java-разработки")
        String title,
        @Schema(description = "ID текущего активного модуля (первого не пройденного)", example = "2")
        Long currentModuleId,
        @Schema(description = "Всего модулей в курсе", example = "10")
        long totalModules,
        @Schema(description = "Всего уроков в курсе", example = "50")
        long totalLessons,
        @Schema(description = "Пройдено модулей", example = "1")
        long completedModules,
        @Schema(description = "Пройдено уроков", example = "5")
        long completedLessons
) {
}
