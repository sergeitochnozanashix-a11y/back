package software.pxel.learneasy.api.dto.attempt;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import software.pxel.learneasy.model.enums.AnswerInputType;

@Schema(description = "Создаваемый ответ по вопросу")
public record AttemptAnswerCreate(
        @Schema(description = "ID вопроса", example = "101")
        @NotNull @Positive
        Long questionId,

        @Schema(description = "Тип ввода", example = "TEXT")
        @NotNull
        AnswerInputType inputType,

        @Schema(description = "Текст ответа (для TEXT), макс. 2000 символов")
        @Size(max = 2_000, message = "Text answer cannot exceed 2000 characters")
        String textAnswer,

        @Schema(description = "URL голосового файла (для VOICE)")
        String voiceFileUrl,

        @Schema(description = "Транскрипт (для VOICE, может быть null при создании)")
        String voiceTranscript
) {
}
