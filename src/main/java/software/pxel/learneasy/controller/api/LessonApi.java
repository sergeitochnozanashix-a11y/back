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
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import software.pxel.learneasy.api.dto.common.ErrorResponse;
import software.pxel.learneasy.api.dto.lesson.*;

@Tag(name = "Lesson API", description = "API для управления уроками в модулях")
@SecurityRequirement(name = "bearerAuth")
public interface LessonApi {

    @Operation(
            summary = "Получить все уроки модуля",
            description = "Возвращает список всех уроков для указанного модуля",
            responses = {
                    @ApiResponse(responseCode = "200", description = "Успешный ответ",
                            content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                                    schema = @Schema(implementation = PageLessonWithoutContent.class))),
                    @ApiResponse(responseCode = "401", description = "Неавторизованный доступ",
                            content = @Content),
                    @ApiResponse(responseCode = "404", description = "Модуль не найден",
                            content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                                    schema = @Schema(implementation = ErrorResponse.class))),
                    @ApiResponse(responseCode = "500", description = "Внутренняя ошибка сервера",
                            content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                                    schema = @Schema(implementation = ErrorResponse.class))
                    )
            }
    )
    ResponseEntity<Page<LessonWithoutContentList>> getLessonsByModuleId(
            @Parameter(description = "id модуля", example = "1")
            @PathVariable Long moduleId,

            @Parameter(description = "Номер страницы (начиная с 0)", example = "0")
            @RequestParam(defaultValue = "0") int page,

            @Parameter(description = "Размер страницы", example = "10")
            @RequestParam(defaultValue = "10") int size);

    @Operation(
            summary = "Получить урок по ID",
            description = "Возвращает урок по указанному идентификатору, включая его контент в актуальном формате.",
            responses = {
                    @ApiResponse(responseCode = "200", description = "Успешный ответ",
                            content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                                    schema = @Schema(implementation = LessonWithContentList.class))),
                    @ApiResponse(responseCode = "401", description = "Неавторизованный доступ",
                            content = @Content),
                    @ApiResponse(responseCode = "404", description = "Урок не найден",
                            content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                                    schema = @Schema(implementation = ErrorResponse.class))),
                    @ApiResponse(responseCode = "500", description = "Внутренняя ошибка сервера",
                            content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                                    schema = @Schema(implementation = ErrorResponse.class)))
            }
    )
    ResponseEntity<LessonWithContentList> getLessonById(Long id);

    @Operation(
            summary = "Создать новый урок",
            description = "Создает новый урок в указанном модуле. Контент можно передать либо в поле 'content' (Markdown), либо в 'contentBlocks' (JSON). 'content' имеет приоритет.",
            responses = {
                    @ApiResponse(responseCode = "201", description = "Урок успешно создан",
                            content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                                    schema = @Schema(implementation = LessonWithContentList.class))),
                    @ApiResponse(responseCode = "400", description = "Неверные данные урока",
                            content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                                    schema = @Schema(implementation = ErrorResponse.class))),
                    @ApiResponse(responseCode = "401", description = "Неавторизованный доступ",
                            content = @Content),
                    @ApiResponse(responseCode = "404", description = "Модуль не найден",
                            content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                                    schema = @Schema(implementation = ErrorResponse.class))),
                    @ApiResponse(responseCode = "500", description = "Внутренняя ошибка сервера",
                            content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                                    schema = @Schema(implementation = ErrorResponse.class)))
            }
    )
    ResponseEntity<LessonWithContentList> createLesson(LessonRequest lesson);

    @Operation(
            summary = "Обновить урок",
            description = "Обновляет существующий урок. Контент можно передать либо в поле 'content' (Markdown), либо в 'contentBlocks' (JSON). 'content' имеет приоритет.",
            responses = {
                    @ApiResponse(responseCode = "200", description = "Урок успешно обновлен",
                            content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                                    schema = @Schema(implementation = LessonWithContentList.class))),
                    @ApiResponse(responseCode = "400", description = "Неверные данные урока",
                            content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                                    schema = @Schema(implementation = ErrorResponse.class))),
                    @ApiResponse(responseCode = "401", description = "Неавторизованный доступ",
                            content = @Content),
                    @ApiResponse(responseCode = "404", description = "Урок не найден",
                            content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                                    schema = @Schema(implementation = ErrorResponse.class))),
                    @ApiResponse(responseCode = "500", description = "Внутренняя ошибка сервера",
                            content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                                    schema = @Schema(implementation = ErrorResponse.class)))
            }
    )
    ResponseEntity<LessonWithContentList> updateLesson(Long id, UpdateLessonRequest lesson);

    @Operation(
            summary = "Удалить урок",
            description = "Удаляет урок по указанному идентификатору",
            responses = {
                    @ApiResponse(responseCode = "204", description = "Урок успешно удален",
                            content = @Content),
                    @ApiResponse(responseCode = "401", description = "Неавторизованный доступ",
                            content = @Content),
                    @ApiResponse(responseCode = "404", description = "Урок не найден",
                            content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                                    schema = @Schema(implementation = ErrorResponse.class))),
                    @ApiResponse(responseCode = "500", description = "Внутренняя ошибка сервера",
                            content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                                    schema = @Schema(implementation = ErrorResponse.class)))
            }
    )
    ResponseEntity<Void> deleteLesson(Long id);

    @Operation(
            summary = "Обновить контент урока (JSON формат)",
            description = "Обновляет контент существующего урока, используя старый формат JSON. При использовании этого метода поле с Markdown-контентом будет очищено. Рекомендуется использовать новый эндпоинт для Markdown.",
            deprecated = true,
            responses = {
                    @ApiResponse(responseCode = "200", description = "Урок успешно обновлен",
                            content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                                    schema = @Schema(implementation = LessonWithContentList.class))),
                    @ApiResponse(responseCode = "400", description = "Неверные данные контента",
                            content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                                    schema = @Schema(implementation = ErrorResponse.class))),
                    @ApiResponse(responseCode = "401", description = "Неавторизованный доступ",
                            content = @Content),
                    @ApiResponse(responseCode = "404", description = "Урок не найден",
                            content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                                    schema = @Schema(implementation = ErrorResponse.class))),
                    @ApiResponse(responseCode = "500", description = "Внутренняя ошибка сервера",
                            content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                                    schema = @Schema(implementation = ErrorResponse.class)))
            }
    )
    ResponseEntity<LessonWithContentList> updateLessonContent(
            @Parameter(description = "ID урока", example = "1")
            @PathVariable Long lessonId,
            @Parameter(description = "Обновленный контент урока в формате JSON", required = true)
            @Valid @RequestBody LessonContentUpdateRequest request);

    @Operation(
            summary = "Обновить контент урока (Markdown формат)",
            description = "Обновляет контент существующего урока, используя новый формат Markdown. При использовании этого метода поле с JSON-контентом (contentBlocks) будет очищено.",
            responses = {
                    @ApiResponse(responseCode = "200", description = "Урок успешно обновлен",
                            content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                                    schema = @Schema(implementation = LessonWithContentList.class))),
                    @ApiResponse(responseCode = "400", description = "Неверные данные контента",
                            content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                                    schema = @Schema(implementation = ErrorResponse.class))),
                    @ApiResponse(responseCode = "401", description = "Неавторизованный доступ",
                            content = @Content),
                    @ApiResponse(responseCode = "404", description = "Урок не найден",
                            content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                                    schema = @Schema(implementation = ErrorResponse.class))),
                    @ApiResponse(responseCode = "500", description = "Внутренняя ошибка сервера",
                            content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                                    schema = @Schema(implementation = ErrorResponse.class)))
            }
    )
    ResponseEntity<LessonWithContentList> updateLessonMarkdownContent(
            @Parameter(description = "ID урока", example = "1")
            @PathVariable Long lessonId,
            @Parameter(description = "Обновленный контент урока в формате Markdown", required = true)
            @Valid @RequestBody LessonMarkdownContentUpdateRequest request
    );

    @Operation(
            summary = "Получить порядковые номера для урока",
            description = "Возвращает lessonSequenceOrder (в модуле) и moduleSequenceOrder (в курсе) по lessonId",
            responses = {
                    @ApiResponse(responseCode = "200", description = "Успешный ответ",
                            content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                                    schema = @Schema(implementation = LessonSequenceDTO.class))),
                    @ApiResponse(responseCode = "401", description = "Неавторизованный доступ", content = @Content),
                    @ApiResponse(responseCode = "404", description = "Урок не найден",
                            content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                                    schema = @Schema(implementation = ErrorResponse.class))),
                    @ApiResponse(responseCode = "500", description = "Внутренняя ошибка сервера",
                            content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                                    schema = @Schema(implementation = ErrorResponse.class)))
            }
    )
    ResponseEntity<LessonSequenceDTO> getLessonSequenceOrders(
            @Parameter(description = "ID урока", example = "123") Long lessonId
    );
}
