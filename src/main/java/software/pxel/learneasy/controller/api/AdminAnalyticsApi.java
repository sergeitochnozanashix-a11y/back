package software.pxel.learneasy.controller.api;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestParam;
import software.pxel.learneasy.api.dto.admin.response.AnalyticResponse;
import software.pxel.learneasy.model.enums.Period;

@SecurityRequirement(name = "bearerAuth")
@Tag(name = "Analytic API", description = "API для аналитики приложения.")
public interface AdminAnalyticsApi {

    @Operation(
            summary = "Получить сводную аналитику для дашборда администратора",
            description = "Возвращает агрегированную статистику по ключевым метрикам платформы, " +
                    "включая данные о пользователях, их активности, удержании, " +
                    "а также таблицу лидеров по прогрессу и график роста.",
            security = @SecurityRequirement(name = "bearerAuth")
    )
    @Parameter(
            name = "period",
            description = "Период для расчета статистики. По умолчанию 'month'.",
            in = io.swagger.v3.oas.annotations.enums.ParameterIn.QUERY,
            schema = @Schema(implementation = Period.class)
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200",
                    description = "Аналитические данные успешно получены.",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = AnalyticResponse.class))
            ),
            @ApiResponse(responseCode = "401", description = "Неавторизованный доступ.", content = @Content),
            @ApiResponse(responseCode = "403", description = "Доступ запрещен. Требуется роль ADMIN.", content = @Content)
    })
    ResponseEntity<AnalyticResponse> getAnalyticsDashboard(@RequestParam(value = "period", defaultValue = "month") String period);
}
