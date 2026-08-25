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
import software.pxel.learneasy.api.dto.common.ErrorResponse;
import software.pxel.learneasy.api.dto.attempt.CreateAttemptRequest;
import software.pxel.learneasy.api.dto.attempt.TestAttemptResponse;
import software.pxel.learneasy.model.User;

@Tag(name = "Test Attempts API", description = "API для управления попытками прохождения тестов")
@SecurityRequirement(name = "bearerAuth")
public interface TestAttemptApi {

    @Operation(
            summary = "Создать попытку (полная отправка)",
            description = "Создает попытку для указанного теста и все ответы пользователя.",
            responses = {
                    @ApiResponse(responseCode = "201", description = "Попытка успешно создана",
                            content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                                    schema = @Schema(implementation = TestAttemptResponse.class))),
                    @ApiResponse(responseCode = "400", description = "Неверные данные ответов",
                            content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                                    schema = @Schema(implementation = ErrorResponse.class))),
                    @ApiResponse(responseCode = "401", description = "Неавторизованный доступ"),
                    @ApiResponse(responseCode = "404", description = "Тест/вопрос не найден",
                            content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                                    schema = @Schema(implementation = ErrorResponse.class))),
                    @ApiResponse(responseCode = "409", description = "Конфликт валидации ответов",
                            content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                                    schema = @Schema(implementation = ErrorResponse.class))),
                    @ApiResponse(responseCode = "500", description = "Внутренняя ошибка сервера",
                            content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                                    schema = @Schema(implementation = ErrorResponse.class)))
            }
    )
    ResponseEntity<TestAttemptResponse> createAttempt(
            @Parameter(hidden = true) User currentUser,
            @Valid @RequestBody CreateAttemptRequest request
    );

    @Operation(
            summary = "Получить последнюю попытку по тесту",
            description = "Возвращает последнюю попытку текущего пользователя для указанного теста.",
            responses = {
                    @ApiResponse(responseCode = "200", description = "Успешный ответ",
                            content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                                    schema = @Schema(implementation = TestAttemptResponse.class))),
                    @ApiResponse(responseCode = "401", description = "Неавторизованный доступ"),
                    @ApiResponse(responseCode = "404", description = "Попытки для теста не найдены",
                            content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                                    schema = @Schema(implementation = ErrorResponse.class)))
            }
    )
    ResponseEntity<TestAttemptResponse> getLastAttempt(
            @Parameter(hidden = true) User currentUser,
            @Parameter(description = "ID теста", required = true, example = "42") Long testId
    );

    @Operation(
            summary = "Получить список попыток по тесту",
            description = "Возвращает постраничный список попыток текущего пользователя для теста.",
            responses = {
                    @ApiResponse(responseCode = "200", description = "Успешный ответ",
                            content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE)),
                    @ApiResponse(responseCode = "401", description = "Неавторизованный доступ")
            }
    )
    ResponseEntity<Page<TestAttemptResponse>> getAttempts(
            @Parameter(hidden = true) User currentUser,
            @Parameter(description = "ID теста", required = true, example = "42") Long testId,
            @Parameter(description = "Номер страницы (0..N)", example = "0") Integer page,
            @Parameter(description = "Размер страницы", example = "20") Integer size
    );
}
