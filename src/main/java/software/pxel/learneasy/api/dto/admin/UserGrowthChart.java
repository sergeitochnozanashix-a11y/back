package software.pxel.learneasy.api.dto.admin;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Данные для графика роста и активности пользователей")
public record UserGrowthChart(
        @Schema(description = "Название текущего календарного месяца на русском языке", example = "Октябрь")
        String currentMonthName,

        @Schema(description = "Количество новых пользователей за последние 30 дней", example = "250")
        long newUsersMonthly,

        @Schema(description = "Количество активных пользователей за последние 30 дней", example = "500")
        long activeUsersMonthly
) {
}
