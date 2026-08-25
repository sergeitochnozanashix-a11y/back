package software.pxel.learneasy.api.dto.test;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
@Schema(description = "DTO для информации об экзамене по модулю.")
public class ExamDTO {

    @NotNull
    @Schema(description = "ID теста.", example = "101")
    private Long testId;

    @NotNull
    @Schema(description = "ID модуля, к которому относится экзамен.", example = "202")
    private Long moduleId;

    @Schema(description = "Порядковый номер модуля в курсе.", example = "1")
    private Integer moduleSequenceOrder;

    @NotNull
    @Schema(description = "Название модуля.", example = "Введение в Java")
    private String moduleTitle;

    @NotNull
    @Schema(description = "Общее количество вопросов в тесте.", example = "15")
    private Integer questionsCount;

    @NotNull
    @Schema(description = "Процент правильных ответов для прохождения.", example = "70")
    private Integer passThresholdPercentage;

    @NotNull
    @Schema(description = "Статус экзамена", allowableValues = {"BLOCKED", "PASSED", "FAILED", "AVAILABLE"}, example = "BLOCKED")
    private String status;

    @Schema(description = "Результат последней попытки сдачи.")
    private LastResultDTO lastResult;
}
