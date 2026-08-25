package software.pxel.learneasy.api.dto.test;

import io.swagger.v3.oas.annotations.media.Schema;
import software.pxel.learneasy.api.dto.question.QuestionResponse;

import java.util.List;

@Schema(description = "Модель ответа для теста с вопросами")
public record TestResponse(
        @Schema(description = "Уникальный идентификатор теста", example = "1")
        Long id,

        @Schema(description = "Уникальный идентификатор урока, к которому относится тест", example = "1")
        Long lessonId,

        @Schema(description = "Уникальный идентификатор модуля, к которому относится тест", example = "1")
        Long moduleId,

        @Schema(description = "Порядковый номер урока в модуле", example = "3")
        Integer lessonSequenceOrder,

        @Schema(description = "Порядковый номер модуля в курсе", example = "1")
        Integer moduleSequenceOrder,

        @Schema(description = "Тип созданного теста", example = "LESSON_TEST")
        String testType,

        @Schema(description = "Название теста", example = "Final Test for Java Basics")
        String title,

        @Schema(description = "Процент правильных ответов для прохождения теста", example = "70")
        Integer passThresholdPercentage,

        @Schema(description = "Список вопросов теста")
        List<QuestionResponse> questions
) {
}
