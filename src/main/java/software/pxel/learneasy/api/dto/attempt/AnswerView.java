package software.pxel.learneasy.api.dto.attempt;

import io.swagger.v3.oas.annotations.media.Schema;
import software.pxel.learneasy.model.enums.AnswerEvaluation;
import software.pxel.learneasy.model.enums.AnswerInputType;

@Schema(description = "Ответ по вопросу для клиента")
public record AnswerView(
        Long questionId,
        String questionText,
        AnswerInputType inputType,
        String textAnswer,
        String voiceFileUrl,
        String voiceTranscript,
        AnswerEvaluation evaluation,
        String notes
) {
}
