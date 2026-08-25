package software.pxel.learneasy.api.dto.mainpage;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;

@Schema(description = "Агрегированный ответ для главной страницы пользователя")
public record MainPageInfoResponse(
        @Schema(description = "Информация о текущем курсе пользователя")
        CourseInfoDTO courseInfo,

        @Schema(description = "Список модулей текущего курса с прогрессом")
        List<ModuleInfoDTO> modules,

        @Schema(description = """
                Ежедневная активность пользователя за последние 7 дней (включая сегодня).
                Если в какой-то день активности не было — значения равны 0.
                """)
        List<UserActivityStatsResponse> weeklyActivity
) {
}
