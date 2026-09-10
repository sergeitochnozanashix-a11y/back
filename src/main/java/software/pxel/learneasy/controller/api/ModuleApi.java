package software.pxel.learneasy.controller.api;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.parameters.RequestBody;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.data.domain.Page;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import software.pxel.learneasy.api.dto.common.ErrorResponse;
import software.pxel.learneasy.api.dto.module.*;
import software.pxel.learneasy.model.User;

import java.time.LocalDateTime;

@Tag(name = "Module API", description = "API для управления учебными модулями")
@SecurityRequirement(name = "bearerAuth")
public interface ModuleApi {

    @Operation(
            summary = "Получить все модули",
            description = "Возвращает список всех учебных модулей с пагинацией и фильтрацией",
            responses = {
                    @ApiResponse(responseCode = "200", description = "Успешный ответ",
                            content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                                    schema = @Schema(implementation = PageModuleResponse.class))),
                    @ApiResponse(responseCode = "401", description = "Неавторизованный доступ",
                            content = @Content),
                    @ApiResponse(responseCode = "500", description = "Внутренняя ошибка сервера",
                            content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                                    schema = @Schema(implementation = ErrorResponse.class)))
            }
    )
    ResponseEntity<Page<ModuleResponse>> getAllModules(
            @Parameter(description = "Фильтр по названию модуля (регистронезависимый)")
            @RequestParam(required = false) String title,

            @Parameter(description = "Фильтр по описанию модуля")
            @RequestParam(required = false) String description,

            @Parameter(description = "Фильтр по ID курса")
            @RequestParam(required = false) Long courseId,

            @Parameter(description = "Дата создания после (включительно)", example = "2025-01-01T00:00:00")
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime createdAfter,

            @Parameter(description = "Дата создания до (включительно)", example = "2025-12-31T23:59:59")
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime createdBefore,

            @Parameter(description = "Номер страницы (начиная с 0)", example = "0")
            @RequestParam(defaultValue = "0") int page,

            @Parameter(description = "Размер страницы", example = "10")
            @RequestParam(defaultValue = "10") int size,

            @Parameter(
                    description = "Поле и направление сортировки (формат: поле,направление)",
                    example = "title,asc",
                    schema = @Schema(type = "string", allowableValues = {"id,asc", "id,desc", "title,asc", "title,desc", "description,asc", "description,desc", "sequenceOrder,asc", "sequenceOrder,desc", "createdAt,asc", "createdAt,desc", "updatedAt,asc", "updatedAt,desc"})
            )
            @RequestParam(defaultValue = "title,asc") String sort);

    @Operation(
            summary = "Получить модуль по ID",
            description = "Возвращает модуль по указанному идентификатору",
            responses = {
                    @ApiResponse(responseCode = "200", description = "Успешный ответ",
                            content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                                    schema = @Schema(implementation = ModuleWithLessonList.class))),
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
    ResponseEntity<ModuleWithLessonList> getModuleById(Long id);

    @Operation(
            summary = "Создать новый модуль",
            description = "Создает новый учебный модуль",
            responses = {
                    @ApiResponse(responseCode = "201", description = "Модуль успешно создан",
                            content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                                    schema = @Schema(implementation = ModuleResponse.class))),
                    @ApiResponse(responseCode = "400", description = "Неверные данные модуля",
                            content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                                    schema = @Schema(implementation = ErrorResponse.class))),
                    @ApiResponse(responseCode = "401", description = "Неавторизованный доступ",
                            content = @Content),
                    @ApiResponse(responseCode = "500", description = "Внутренняя ошибка сервера",
                            content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                                    schema = @Schema(implementation = ErrorResponse.class)))
            }
    )
    ResponseEntity<ModuleResponse> createModule(@RequestBody ModuleRequest module);

    @Operation(
            summary = "Обновить модуль",
            description = "Обновляет существующий учебный модуль",
            responses = {
                    @ApiResponse(responseCode = "200", description = "Модуль успешно обновлен",
                            content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                                    schema = @Schema(implementation = ModuleResponse.class))),
                    @ApiResponse(responseCode = "400", description = "Неверные данные модуля",
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
    ResponseEntity<ModuleResponse> updateModule(Long id, @RequestBody ModuleRequest module);

    @Operation(
            summary = "Удалить модуль",
            description = "Удаляет учебный модуль по указанному идентификатору",
            responses = {
                    @ApiResponse(responseCode = "204", description = "Модуль успешно удален",
                            content = @Content),
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
    ResponseEntity<Void> deleteModule(Long id);

    @Operation(
            summary = "Получить детальную информацию о модуле с прогрессом",
            description = "Возвращает детальную информацию о конкретном модуле и прогресс пользователя по каждому уроку.",
            responses = {
                    @ApiResponse(responseCode = "200", description = "Успешный ответ",
                            content = @Content(mediaType = "application/json",
                                    schema = @Schema(implementation = ModuleDetailsDTO.class))),
                    @ApiResponse(responseCode = "404", description = "Модуль или курс не найдены"),
                    @ApiResponse(responseCode = "401", description = "Неавторизованный доступ")
            }
    )
    ModuleDetailsDTO getModuleDetailsWithProgress(
            @Parameter(description = "ID курса", example = "101")
            @PathVariable Long courseId,
            @Parameter(description = "ID модуля", example = "101")
            @PathVariable Long moduleId,
            @AuthenticationPrincipal
            @Parameter(hidden = true) User currentUser
    );
}
