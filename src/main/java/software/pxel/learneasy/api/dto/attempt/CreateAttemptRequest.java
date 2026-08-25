package software.pxel.learneasy.api.dto.attempt;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.util.List;

@Schema(description = "Создание попытки (полная/частичная отправка)")
public record CreateAttemptRequest(
        @Schema(description = "ID теста", example = "42", requiredMode = Schema.RequiredMode.REQUIRED)
        @NotNull @Positive
        Long testId,

        @Schema(
                description = "Ответы пользователя. Может быть пустым/отсутствовать для пропуска.",
                requiredMode = Schema.RequiredMode.NOT_REQUIRED
        )
        @Valid
        List<AttemptAnswerCreate> answers
) {
}
