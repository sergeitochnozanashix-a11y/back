package software.pxel.learneasy.controller.api;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.parameters.RequestBody;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import software.pxel.learneasy.api.dto.common.ErrorResponse;
import software.pxel.learneasy.api.dto.test.ExamDTO;
import software.pxel.learneasy.api.dto.test.TestRequest;
import software.pxel.learneasy.api.dto.test.TestResponse;
import software.pxel.learneasy.model.User;

@Tag(name = "Test API", description = "API для управления тестами")
@SecurityRequirement(name = "bearerAuth")
public interface TestApi {

    @Operation(
            summary = "Создать новый тест",
            description = "Создает новый тест с вопросами для указанного урока.",
            responses = {
                    @ApiResponse(responseCode = "201", description = "Тест успешно создан",
                            content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                                    schema = @Schema(implementation = TestResponse.class))),
                    @ApiResponse(responseCode = "400", description = "Неверные данные теста или вопросов",
                            content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                                    schema = @Schema(implementation = ErrorResponse.class))),
                    @ApiResponse(responseCode = "401", description = "Неавторизованный доступ"),
                    @ApiResponse(responseCode = "404", description = "Урок не найден"),
                    @ApiResponse(responseCode = "409", description = "Тест для данного урока уже существует"),
                    @ApiResponse(responseCode = "500", description = "Внутренняя ошибка сервера",
                            content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                                    schema = @Schema(implementation = ErrorResponse.class)))
            }
    )
    ResponseEntity<TestResponse> createTest(@Valid @RequestBody TestRequest testRequest);

    @Operation(
            summary = "Получить тест по ID",
            description = "Возвращает тест с вопросами по указанному идентификатору.",
            responses = {
                    @ApiResponse(responseCode = "200", description = "Успешный ответ",
                            content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                                    schema = @Schema(implementation = TestResponse.class))),
                    @ApiResponse(responseCode = "401", description = "Неавторизованный доступ"),
                    @ApiResponse(responseCode = "404", description = "Тест не найден",
                            content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                                    schema = @Schema(implementation = ErrorResponse.class))),
                    @ApiResponse(responseCode = "500", description = "Внутренняя ошибка сервера",
                            content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                                    schema = @Schema(implementation = ErrorResponse.class)))
            }
    )
    ResponseEntity<TestResponse> getTestById(
            @Parameter(description = "ID теста", required = true, example = "1") Long id
    );

    @Operation(
            summary = "Получить тест по ID урока",
            description = "Возвращает тест с вопросами для указанного урока.",
            responses = {
                    @ApiResponse(responseCode = "200", description = "Успешный ответ",
                            content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                                    schema = @Schema(implementation = TestResponse.class))),
                    @ApiResponse(responseCode = "401", description = "Неавторизованный доступ"),
                    @ApiResponse(responseCode = "404", description = "Тест для урока не найден",
                            content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                                    schema = @Schema(implementation = ErrorResponse.class))),
                    @ApiResponse(responseCode = "500", description = "Внутренняя ошибка сервера",
                            content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                                    schema = @Schema(implementation = ErrorResponse.class)))
            }
    )
    ResponseEntity<TestResponse> getTestByLessonId(
            @Parameter(description = "ID урока", required = true, example = "1") Long lessonId
    );

    @Operation(
            summary = "Обновить тест",
            description = "Обновляет существующий тест и его вопросы.",
            responses = {
                    @ApiResponse(responseCode = "200", description = "Тест успешно обновлен",
                            content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                                    schema = @Schema(implementation = TestResponse.class))),
                    @ApiResponse(responseCode = "400", description = "Неверные данные теста или вопросов",
                            content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                                    schema = @Schema(implementation = ErrorResponse.class))),
                    @ApiResponse(responseCode = "401", description = "Неавторизованный доступ"),
                    @ApiResponse(responseCode = "404", description = "Тест или связанный урок не найден",
                            content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                                    schema = @Schema(implementation = ErrorResponse.class))),
                    @ApiResponse(responseCode = "409", description = "Конфликт: другой тест уже привязан к новому ID урока"),
                    @ApiResponse(responseCode = "500", description = "Внутренняя ошибка сервера",
                            content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                                    schema = @Schema(implementation = ErrorResponse.class)))
            }
    )
    ResponseEntity<TestResponse> updateTest(
            @Parameter(description = "ID теста для обновления", required = true, example = "1") Long id,
            @Valid @RequestBody TestRequest testRequest
    );

    @Operation(
            summary = "Удалить тест",
            description = "Удаляет тест и все связанные с ним вопросы.",
            responses = {
                    @ApiResponse(responseCode = "204", description = "Тест успешно удален"),
                    @ApiResponse(responseCode = "401", description = "Неавторизованный доступ"),
                    @ApiResponse(responseCode = "404", description = "Тест не найден",
                            content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                                    schema = @Schema(implementation = ErrorResponse.class))),
                    @ApiResponse(responseCode = "500", description = "Внутренняя ошибка сервера",
                            content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                                    schema = @Schema(implementation = ErrorResponse.class)))
            }
    )
    ResponseEntity<Void> deleteTest(
            @Parameter(description = "ID теста для удаления", required = true, example = "1") Long id
    );

    @Operation(
            summary = "Получить экзамены по курсу",
            description = "Возвращает список экзаменов, связанных с указанным курсом, с поддержкой пагинации. Доступно только для владельца курса.",
            responses = {
                    @ApiResponse(responseCode = "200", description = "Успешный ответ",
                            content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                                    schema = @Schema(implementation = ExamDTO.class))),
                    @ApiResponse(responseCode = "401", description = "Неавторизованный доступ"),
                    @ApiResponse(responseCode = "403", description = "Доступ запрещен (не владелец курса)"),
                    @ApiResponse(responseCode = "404", description = "Курс не найден"),
                    @ApiResponse(responseCode = "500", description = "Внутренняя ошибка сервера")
            }
    )
    ResponseEntity<Page<ExamDTO>> getCourseExams(
            @Parameter(description = "ID курса", required = true, example = "1") Long courseId,
            @Parameter(description = "Номер страницы", example = "0") int page,
            @Parameter(description = "Размер страницы", example = "10") int size,
            @AuthenticationPrincipal @Parameter(hidden = true) User currentUser
    );
}
