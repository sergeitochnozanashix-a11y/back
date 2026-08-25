package software.pxel.learneasy.api.dto.lesson;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Порядковые номера урока в модуле и модуля в курсе, а также ID связанного теста")
public record LessonSequenceDTO(
        @Schema(description = "Порядковый номер урока в модуле", example = "3")
        Integer lessonSequenceOrder,
        @Schema(description = "Порядковый номер модуля в курсе", example = "1")
        Integer moduleSequenceOrder,
        @Schema(description = "Уникальный идентификатор связанного теста", example = "42", nullable = true)
        Long testId
) {
}
