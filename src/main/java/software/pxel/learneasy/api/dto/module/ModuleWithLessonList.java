package software.pxel.learneasy.api.dto.module;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import software.pxel.learneasy.api.dto.lesson.LessonWithoutContentList;

import java.util.List;

@Builder
@Schema(description = "Модель ответа для получения модуля с уроками")
public record ModuleWithLessonList(
        @Schema(description = "Уникальный номер модуля", example = "1") Long id,
        @Schema(description = "Название модуля", example = "Web Development Fundamentals") String title,
        @Schema(description = "Описание модуля", example = "Understand HTML, CSS, and JavaScript to build web pages.") String description,
        // courseId и sequenceOrder обязательны в ModuleRequest, поэтому без них
        // ответ этого метода нельзя было отправить обратно в PUT /modules/{id}.
        @Schema(description = "ID курса, к которому принадлежит модуль", example = "1") Long courseId,
        @Schema(description = "Порядковый номер модуля в курсе", example = "1") Integer sequenceOrder,
        @Schema(description = "Массив уроков") List<LessonWithoutContentList> lessons
) {
}
