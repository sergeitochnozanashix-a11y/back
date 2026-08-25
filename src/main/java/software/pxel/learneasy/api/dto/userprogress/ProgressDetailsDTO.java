package software.pxel.learneasy.api.dto.userprogress;

import io.swagger.v3.oas.annotations.media.Schema;

public record ProgressDetailsDTO(
        @Schema(description = "Статус выполнения теоретического задания", example = "true")
        boolean theoryCompleted,
        @Schema(description = "Статус выполнения голосового задания", example = "true")
        boolean voiceTaskCompleted,
        @Schema(description = "Статус выполнения тестового задания", example = "true")
        boolean testTaskCompleted,
        @Schema(description = "Процент правильных ответов в тесте", example = "95")
        Integer testResult
) {
}
