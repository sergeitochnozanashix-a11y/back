package software.pxel.learneasy.api.dto.test;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import software.pxel.learneasy.api.dto.question.QuestionRequest;
import software.pxel.learneasy.model.enums.TestType;

import java.util.List;

@Schema(description = "Модель запроса для создания/обновления теста")
public record TestRequest(
        @Schema(description = "Тип теста (LESSON_TEST или MODULE_EXAM)", example = "LESSON_TEST", requiredMode = Schema.RequiredMode.REQUIRED)
        @NotNull(message = "Test type cannot be null")
        TestType testType,

        @Schema(description = "Уникальный идентификатор урока, к которому относится тест. Используется только для LESSON_TEST.", example = "1")
        @Positive
        Long lessonId,

        @Schema(description = "Уникальный идентификатор модуля, к которому относится экзамен. Используется только для MODULE_EXAM.", example = "1")
        @Positive
        @NotNull(message = "Module id cannot be null")
        Long moduleId,

        @Schema(description = "Название теста", example = "Final Test for Java Basics", defaultValue = "Test", requiredMode = Schema.RequiredMode.REQUIRED)
        @NotBlank(message = "Test title cannot be blank")
        @Size(min = 3, max = 255, message = "Test title must be between 3 and 255 characters")
        String title,

        @Schema(description = "Процент правильных ответов для прохождения теста (0-100)", example = "70", defaultValue = "70", requiredMode = Schema.RequiredMode.REQUIRED)
        @NotNull(message = "Pass threshold percentage cannot be null")
        @Min(value = 0, message = "Pass threshold percentage must be at least 0")
        @Max(value = 100, message = "Pass threshold percentage must be at most 100")
        Integer passThresholdPercentage,

        @Schema(description = "Список вопросов для теста")
        @Valid
        List<QuestionRequest> questions
) {
}