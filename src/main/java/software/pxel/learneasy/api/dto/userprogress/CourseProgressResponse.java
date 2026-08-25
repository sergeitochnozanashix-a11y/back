package software.pxel.learneasy.api.dto.userprogress;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;

import java.util.List;

@Builder
@Schema(description = "Модель ответа данных прогресса пользователя по прохождению курса")
public record CourseProgressResponse(
        @Schema(description = "Общее количество модулей в курсе", example = "101")
        Long totalModules,
        @Schema(description = "Количество завершенных пользователем модулей в курсе", example = "101")
        Long completedModules,
        @Schema(description = "Общее количество уроков в курсе", example = "101")
        Long totalLessons,
        @Schema(description = "Количество завершенных пользователем уроков в курсе", example = "101")
        Long completedLessons,
        @Schema(description = "Список модулей курса с их статусами", example = "101")
        List<ModuleProgressDTO> modules
) {
}
