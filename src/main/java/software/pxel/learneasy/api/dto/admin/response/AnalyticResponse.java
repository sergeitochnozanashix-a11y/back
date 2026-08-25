package software.pxel.learneasy.api.dto.admin.response;

import io.swagger.v3.oas.annotations.media.Schema;
import software.pxel.learneasy.api.dto.admin.AnalyticsSummary;
import software.pxel.learneasy.api.dto.admin.UserGrowthChart;
import software.pxel.learneasy.api.dto.admin.UserTable;

import java.util.List;

@Schema(description = "Полный ответ с аналитическими данными для дашборда администратора")
public record AnalyticResponse(
        @Schema(description = "Блок со сводными данными для карточек")
        AnalyticsSummary analyticsSummary,

        @Schema(description = "Блок с таблицей последней активности пользователей")
        List<UserTable> userTable,

        @Schema(description = "Блок с данными для графика роста пользователей")
        UserGrowthChart userGrowthChart
) {
}
