package software.pxel.learneasy.controller.api;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import software.pxel.learneasy.api.dto.chat.request.SendMessageRequest;
import software.pxel.learneasy.api.dto.chat.response.ChatMessagesResponse;
import software.pxel.learneasy.api.dto.chat.response.ChatResponse;
import software.pxel.learneasy.api.dto.chat.response.CreateChatResponse;
import software.pxel.learneasy.api.dto.common.ErrorResponse;

@SecurityRequirement(name = "bearerAuth")
@Tag(name = "AI Companion API", description = "API для обращения к чату с нейросетью.")
public interface AICompanionApi {

    @Operation(
            summary = "Создать новый чат",
            description = "Создаёт новый чат для текущего пользователя.",
            responses = {
                    @ApiResponse(responseCode = "201", description = "Чат успешно создан",
                            content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                                    schema = @Schema(implementation = CreateChatResponse.class))
                    ),
                    @ApiResponse(responseCode = "400", description = "Ошибка запроса",
                            content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE)
                    ),
                    @ApiResponse(responseCode = "401", description = "Неавторизованный доступ",
                            content = @Content),
            }
    )
    ResponseEntity<CreateChatResponse> createNewChat();

    @Operation(
            summary = "Возвращает список всех чатов пользователя",
            description = """
                    Возвращает список всех чатов текущего аутентифицированного пользователя,
                    отсортированный по дате обновления (от самых свежих к самым старым).
                    При одинаковой дате обновления сортировка выполняется по дате создания.
                    """,
            responses = {
                    @ApiResponse(
                            responseCode = "200",
                            description = "Список чатов успешно получен",
                            content = @Content(
                                    mediaType = MediaType.APPLICATION_JSON_VALUE,
                                    schema = @Schema(implementation = ChatResponse.class)
                            )
                    ),
                    @ApiResponse(responseCode = "400", description = "Ошибка запроса",
                            content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE)
                    ),
                    @ApiResponse(responseCode = "404", description = "Пользователь не найден",
                            content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                                    schema = @Schema(implementation = ErrorResponse.class))
                    ),
                    @ApiResponse(responseCode = "401", description = "Неавторизованный доступ",
                            content = @Content),
            }
    )
    ResponseEntity<ChatResponse> getListChatsUserSortDate();

    @Operation(
            summary = "Возвращает список всех сообщений чата",
            description = """
                    Возвращает список всех сообщений указанного чата, а также его заголовок.
                    Пользователь может получать сообщения только из своих чатов.
                    Сообщения возвращаются в хронологическом порядке (от старых к новым).
                    """,
            responses = {
                    @ApiResponse(
                            responseCode = "200",
                            description = "Сообщения успешно получены",
                            content = @Content(
                                    mediaType = MediaType.APPLICATION_JSON_VALUE,
                                    schema = @Schema(implementation = ChatMessagesResponse.class)
                            )
                    ),
                    @ApiResponse(responseCode = "400", description = "Ошибка запроса",
                            content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE)
                    ),
                    @ApiResponse(responseCode = "403", description = "Доступ к чату запрещен",
                            content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                                    schema = @Schema(implementation = ErrorResponse.class))
                    ),
                    @ApiResponse(responseCode = "404", description = "Чат не найден",
                            content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                                    schema = @Schema(implementation = ErrorResponse.class))
                    ),
                    @ApiResponse(responseCode = "401", description = "Неавторизованный доступ",
                            content = @Content),
            }
    )
    ResponseEntity<ChatMessagesResponse> getAllMessageByChatId(@PathVariable("chatId") Long chatId);

    @Operation(
            summary = "Отправка сообщения в чат",
            description = """
                    Отправляет новое сообщение в указанный чат.
                    Пользователь может отправлять сообщения только в свои чаты.
                    Запрос принимается в обработку немедленно, фактический ответ от AI генерируется асинхронно.
                    """,
            responses = {
                    @ApiResponse(
                            responseCode = "202",
                            description = "Сообщение успешно принято в обработку",
                            content = @Content
                    ),
                    @ApiResponse(responseCode = "400", description = "Ошибка запроса",
                            content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE)
                    ),
                    @ApiResponse(responseCode = "403", description = "Доступ к чату запрещен",
                            content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                                    schema = @Schema(implementation = ErrorResponse.class))
                    ),
                    @ApiResponse(responseCode = "404", description = "Чат не найден",
                            content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                                    schema = @Schema(implementation = ErrorResponse.class))
                    ),
                    @ApiResponse(responseCode = "401", description = "Неавторизованный доступ",
                            content = @Content),
            }
    )
    ResponseEntity<Void> sendingMessageToChat(@PathVariable("chatId") Long chatId, @RequestBody @Valid SendMessageRequest request);
}
