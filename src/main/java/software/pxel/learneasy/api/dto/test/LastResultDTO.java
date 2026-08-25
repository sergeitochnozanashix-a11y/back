package software.pxel.learneasy.api.dto.test;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
@Schema(description = "Результат последней попытки сдачи экзамена.")
public class LastResultDTO {

    @NotNull
    @Schema(description = "Процент набранных баллов.", example = "85")
    private Integer scorePercent;

    @NotNull
    @Schema(description = "Количество правильных ответов.", example = "17")
    private Integer correct;

    @NotNull
    @Schema(description = "Общее количество вопросов.", example = "20")
    private Integer total;
}