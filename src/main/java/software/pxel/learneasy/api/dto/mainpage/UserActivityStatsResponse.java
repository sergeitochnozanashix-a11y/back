package software.pxel.learneasy.api.dto.mainpage;

import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDate;

@Schema(description = "Модель ответа для статистики активности пользователя за день")
public record UserActivityStatsResponse(
        @Schema(description = "Дата активности", example = "2024-07-20")
        LocalDate date,

        @Schema(description = "Количество пройденных уроков за эту дату", example = "5")
        Long lessonsCompleted,

        @Schema(description = "Количество сданных тестов за эту дату", example = "2")
        Long testsPassed
) {
}
