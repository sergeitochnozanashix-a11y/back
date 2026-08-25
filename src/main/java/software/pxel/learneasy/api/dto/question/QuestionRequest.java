package software.pxel.learneasy.api.dto.question;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record QuestionRequest(
        @Schema(description = "Текст вопроса", example = "What is polymorphism in OOP?", requiredMode = Schema.RequiredMode.REQUIRED)
        @NotBlank(message = "Question text cannot be blank")
        String text,

        @Schema(description = "Порядковый номер вопроса в тесте", example = "1", defaultValue = "0", requiredMode = Schema.RequiredMode.REQUIRED)
        @NotNull(message = "Sequence order cannot be null")
        @Min(value = 0, message = "Sequence order must be non-negative")
        Integer sequenceOrder,

        @Schema(description = "Максимальное количество баллов за правильный ответ на вопрос", example = "2", defaultValue = "2", requiredMode = Schema.RequiredMode.REQUIRED)
        @NotNull(message = "Max score cannot be null")
        @Min(value = 1, message = "Max score must be at least 1")
        Integer maxScore
) {
}
