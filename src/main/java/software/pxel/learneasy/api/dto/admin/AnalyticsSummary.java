package software.pxel.learneasy.api.dto.admin;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Сводные данные по ключевым метрикам (для карточек на дашборде)")
public record AnalyticsSummary(
        @Schema(description = "Статистика по общему числу пользователей")
        TotalUsers totalUsers,

        @Schema(description = "Статистика по активным пользователям за последние 30 дней")
        ActiveUsers activeUsers,

        @Schema(description = "Коэффициент удержания пользователей")
        RetentionRate retentionRate
) {
    @Schema(description = "Метрика с числовым значением и процентным изменением")
    public record TotalUsers(
            @Schema(description = "Абсолютное значение метрики", example = "3000")
            long value,
            @Schema(description = "Изменение в процентах по сравнению с предыдущим 30-дневным периодом", example = "15.0")
            float changePercentage
    ) {}

    @Schema(description = "Метрика с числовым значением и процентным изменением")
    public record ActiveUsers(
            @Schema(description = "Абсолютное значение метрики", example = "500")
            long value,
            @Schema(description = "Изменение в процентах по сравнению с предыдущим 30-дневным периодом", example = "-7.45")
            float changePercentage
    ) {}

    @Schema(description = "Метрика с числовым значением и процентным изменением")
    public record RetentionRate(
            @Schema(description = "Значение метрики в процентах", example = "85")
            int value,
            @Schema(description = "Изменение в процентах по сравнению с предыдущим 30-дневным периодом", example = "5.1")
            float changePercentage
    ) {}
}
