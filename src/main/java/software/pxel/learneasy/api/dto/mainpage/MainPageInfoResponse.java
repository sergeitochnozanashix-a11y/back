package software.pxel.learneasy.api.dto.mainpage;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;

@Schema(description = "Агрегированный ответ для главной страницы пользователя")
public record MainPageInfoResponse(
        @Schema(
                description = """
                        Курс, в котором пользователь занимался последним.
                        null, если активности ещё не было — тогда modules и
                        weeklyActivity тоже пустые.
                        """,
                nullable = true
        )
        CourseInfoDTO courseInfo,

        @Schema(description = "Список модулей текущего курса с прогрессом. Пуст, если courseInfo = null")
        List<ModuleInfoDTO> modules,

        @Schema(description = """
                Ежедневная активность пользователя за последние 7 дней (включая сегодня).
                Если в какой-то день активности не было — значения равны 0.
                """)
        List<UserActivityStatsResponse> weeklyActivity
) {
}
