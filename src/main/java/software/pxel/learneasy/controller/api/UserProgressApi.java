package software.pxel.learneasy.controller.api;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import software.pxel.learneasy.api.dto.common.ErrorResponse;
import software.pxel.learneasy.api.dto.mainpage.MainPageInfoResponse;
import software.pxel.learneasy.api.dto.userprogress.CourseProgressResponse;
import software.pxel.learneasy.model.User;

@Tag(name = "User progress API", description = "API для управления прогрессом пользователя в обучении")
@SecurityRequirement(name = "bearerAuth")
public interface UserProgressApi {

    @Operation(
            summary = "Получить сводку прогресса пользователя по курсу",
            description = "Возвращает агрегированные данные по прохождению курса",
            responses = {
                    @ApiResponse(responseCode = "200", description = "Успешный ответ",
                            content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                                    schema = @Schema(implementation = CourseProgressResponse.class))),
                    @ApiResponse(responseCode = "401", description = "Неавторизованный доступ", content = @Content),
                    @ApiResponse(responseCode = "500", description = "Внутренняя ошибка сервера",
                            content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                                    schema = @Schema(implementation = ErrorResponse.class)))
            }
    )
    ResponseEntity<CourseProgressResponse> getCourseProgress(
            @Parameter(description = "ID пользователя", example = "1")
            @PathVariable Long userId,
            @Parameter(description = "ID курса", example = "1")
            @PathVariable Long courseId);

    @Operation(
            summary = "Получить информацию для главной страницы",
            description = """
                    Если параметр userId указан и текущий пользователь имеет роль ADMIN —
                    возвращаются данные указанного пользователя.
                    Если параметр не указан — возвращаются данные текущего пользователя.
                    Для обычных пользователей userId игнорируется.
                    """,
            responses = {
                    @ApiResponse(responseCode = "200", description = "Успешный ответ",
                            content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                                    schema = @Schema(implementation = MainPageInfoResponse.class))),
                    @ApiResponse(responseCode = "401", description = "Неавторизованный доступ", content = @Content),
                    @ApiResponse(responseCode = "403", description = "Доступ запрещен", content = @Content),
                    @ApiResponse(responseCode = "404", description = "Курс по умолчанию не найден",
                            content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                                    schema = @Schema(implementation = ErrorResponse.class))),
                    @ApiResponse(responseCode = "500", description = "Внутренняя ошибка сервера",
                            content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                                    schema = @Schema(implementation = ErrorResponse.class)))
            }
    )
    ResponseEntity<MainPageInfoResponse> getMainPageInfo(
            @Parameter(description = "Необязательный ID пользователя (только для ADMIN)", example = "1")
            @RequestParam(required = false) Long userId,
            @AuthenticationPrincipal @Parameter(hidden = true) User currentUser
    );
}
