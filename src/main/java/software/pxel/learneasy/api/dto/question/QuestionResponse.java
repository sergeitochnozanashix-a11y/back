package software.pxel.learneasy.api.dto.question;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Модель ответа для вопроса")
public record QuestionResponse(
        @Schema(description = "Уникальный идентификатор вопроса", example = "42")
        Long questionId,

        @Schema(description = "Текст вопроса", example = "What is polymorphism in OOP?")
        String text,

        @Schema(description = "Порядковый номер вопроса в тесте", example = "1")
        Integer sequenceOrder,

        @Schema(description = "Максимальное количество баллов за правильный ответ на вопрос", example = "2")
        Integer maxScore
) {
}
