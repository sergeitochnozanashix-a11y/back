package software.pxel.learneasy.controller.api;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.parameters.RequestBody;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.data.domain.Page;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import software.pxel.learneasy.api.dto.common.ErrorResponse;
import software.pxel.learneasy.api.dto.course.CourseRequest;
import software.pxel.learneasy.api.dto.course.CourseResponse;
import software.pxel.learneasy.api.dto.course.PageCourseResponse;
import software.pxel.learneasy.api.dto.module.ModuleWithProgressDTO;
import software.pxel.learneasy.model.User;

import java.util.List;

@Tag(name = "Course API", description = "API для управления учебными курсами")
@SecurityRequirement(name = "bearerAuth")
public interface CourseApi {

    @Operation(
            summary = "Получить все курсы",
            description = "Возвращает список всех учебных курсов с пагинацией и фильтрацией",
            responses = {
                    @ApiResponse(responseCode = "200", description = "Успешный ответ",
                            content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                                    schema = @Schema(implementation = PageCourseResponse.class))),
                    @ApiResponse(responseCode = "401", description = "Неавторизованный доступ",
                            content = @Content),
                    @ApiResponse(responseCode = "500", description = "Внутренняя ошибка сервера",
                            content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                                    schema = @Schema(implementation = ErrorResponse.class)))
            }
    )
    ResponseEntity<Page<CourseResponse>> getAllCourses(
            @Parameter(description = "Номер страницы (начиная с 0)", example = "0")
            @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "Размер страницы", example = "10")
            @RequestParam(defaultValue = "10") int size);

    @Operation(
            summary = "Получить курс по ID",
            description = "Возвращает курс по указанному идентификатору",
            responses = {
                    @ApiResponse(responseCode = "200", description = "Успешный ответ",
                            content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                                    schema = @Schema(implementation = CourseResponse.class))),
                    @ApiResponse(responseCode = "401", description = "Неавторизованный доступ",
                            content = @Content),
                    @ApiResponse(responseCode = "404", description = "Курс не найден",
                            content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                                    schema = @Schema(implementation = ErrorResponse.class))),
                    @ApiResponse(responseCode = "500", description = "Внутренняя ошибка сервера",
                            content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                                    schema = @Schema(implementation = ErrorResponse.class)))
            }
    )
    ResponseEntity<CourseResponse> getCourseById(Long id);

    @Operation(
            summary = "Создать новый курс",
            description = "Создает новый учебный курс",
            responses = {
                    @ApiResponse(responseCode = "201", description = "Курс успешно создан",
                            content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                                    schema = @Schema(implementation = CourseResponse.class))),
                    @ApiResponse(responseCode = "400", description = "Неверные данные курса",
                            content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                                    schema = @Schema(implementation = ErrorResponse.class))),
                    @ApiResponse(responseCode = "401", description = "Неавторизованный доступ",
                            content = @Content),
                    @ApiResponse(responseCode = "500", description = "Внутренняя ошибка сервера",
                            content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                                    schema = @Schema(implementation = ErrorResponse.class)))
            }
    )
    ResponseEntity<CourseResponse> createCourse(@RequestBody CourseRequest course);

    @Operation(
            summary = "Обновить курс",
            description = "Обновляет существующий учебный курс",
            responses = {
                    @ApiResponse(responseCode = "200", description = "Курс успешно обновлен",
                            content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                                    schema = @Schema(implementation = CourseResponse.class))),
                    @ApiResponse(responseCode = "400", description = "Неверные данные курса",
                            content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                                    schema = @Schema(implementation = ErrorResponse.class))),
                    @ApiResponse(responseCode = "401", description = "Неавторизованный доступ",
                            content = @Content),
                    @ApiResponse(responseCode = "404", description = "Курс не найден",
                            content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                                    schema = @Schema(implementation = ErrorResponse.class))),
                    @ApiResponse(responseCode = "500", description = "Внутренняя ошибка сервера",
                            content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                                    schema = @Schema(implementation = ErrorResponse.class)))
            }
    )
    ResponseEntity<CourseResponse> updateCourse(Long id, @RequestBody CourseRequest course);

    @Operation(
            summary = "Удалить курс",
            description = "Удаляет учебный курс по указанному идентификатору",
            responses = {
                    @ApiResponse(responseCode = "204", description = "Курс успешно удален",
                            content = @Content),
                    @ApiResponse(responseCode = "401", description = "Неавторизованный доступ",
                            content = @Content),
                    @ApiResponse(responseCode = "404", description = "Курс не найден",
                            content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                                    schema = @Schema(implementation = ErrorResponse.class))),
                    @ApiResponse(responseCode = "500", description = "Внутренняя ошибка сервера",
                            content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                                    schema = @Schema(implementation = ErrorResponse.class)))
            }
    )
    ResponseEntity<Void> deleteCourse(Long id);

    @Operation(
            summary = "Получить модули курса с прогрессом пользователя",
            description = "Возвращает список всех модулей для указанного курса с информацией о прогрессе текущего пользователя по каждому модулю.",
            responses = {
                    @ApiResponse(
                            responseCode = "200", description = "Успешный ответ",
                            content = @Content(
                                    mediaType = MediaType.APPLICATION_JSON_VALUE,
                                    array = @ArraySchema(schema = @Schema(implementation = ModuleWithProgressDTO.class))
                            )
                    ),
                    @ApiResponse(responseCode = "401", description = "Неавторизованный доступ",
                            content = @Content),
                    @ApiResponse(responseCode = "404", description = "Курс не найден",
                            content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                                    schema = @Schema(implementation = ErrorResponse.class))),
                    @ApiResponse(responseCode = "500", description = "Внутренняя ошибка сервера",
                            content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                                    schema = @Schema(implementation = ErrorResponse.class)))
            }
    )
    @GetMapping("/{courseId}/modules")
    ResponseEntity<List<ModuleWithProgressDTO>> getModulesWithProgress(
            @Parameter(description = "ID курса", example = "1")
            @PathVariable Long courseId,
            @AuthenticationPrincipal @Parameter(hidden = true) User currentUser
    );
}
