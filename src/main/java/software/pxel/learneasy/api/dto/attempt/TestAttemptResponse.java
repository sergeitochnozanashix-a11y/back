package software.pxel.learneasy.api.dto.attempt;

import io.swagger.v3.oas.annotations.media.Schema;
import software.pxel.learneasy.model.enums.AttemptStatus;

import java.time.Instant;
import java.util.List;

@Schema(description = "Ответ API: попытка теста")
public record TestAttemptResponse(
        Long id,
        Long userId,
        Long testId,
        Long lessonId,
        Long moduleId,
        AttemptStatus status,
        List<AnswerView> answers,
        Integer scorePercent,
        Boolean passed,
        Instant createdAt,
        Instant updatedAt,
        Instant evaluatedAt,
        String failedReason
) {
}
