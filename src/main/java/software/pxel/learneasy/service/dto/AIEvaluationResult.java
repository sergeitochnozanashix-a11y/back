package software.pxel.learneasy.service.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import software.pxel.learneasy.model.enums.AnswerEvaluation;

@JsonIgnoreProperties(ignoreUnknown = true)
public record AIEvaluationResult(
        @JsonProperty("evaluation") AnswerEvaluation evaluation,
        @JsonProperty("feedback") String feedback
) {
}
