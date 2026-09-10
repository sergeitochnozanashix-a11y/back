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
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import software.pxel.learneasy.api.dto.common.ErrorResponse;
import software.pxel.learneasy.api.dto.user.PageUserResponse;
import software.pxel.learneasy.api.dto.user.UpdateProfileDTO;
import software.pxel.learneasy.api.dto.user.UpdateUserDTO;
import software.pxel.learneasy.api.dto.user.UserDTO;
import software.pxel.learneasy.model.User;

@Tag(name = "User API", description = "API для управления пользователями")
@SecurityRequirement(name = "bearerAuth")
public interface UserApi {

    @Operation(
            summary = "Получить собственный профиль",
            description = "Возвращает профиль текущего пользователя. Идентификатор берётся из токена, "
                    + "в пути не передаётся - прочитать чужой профиль этим методом нельзя. "
                    + "Профильные поля могут быть null, пока пользователь их не заполнил.",
            responses = {
                    @ApiResponse(responseCode = "200", description = "Успешный ответ",
                            content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                                    schema = @Schema(implementation = UserDTO.class))),
                    @ApiResponse(responseCode = "401", description = "Неавторизованный доступ",
                            content = @Content),
                    @ApiResponse(responseCode = "500", description = "Внутренняя ошибка сервера",
                            content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                                    schema = @Schema(implementation = ErrorResponse.class)))
            }
    )
    ResponseEntity<UserDTO> getCurrentUser(
            @AuthenticationPrincipal
            @Parameter(hidden = true) User currentUser);

    @Operation(
            summary = "Обновить собственный профиль",
            description = "Частичное обновление: незаданные (null) поля остаются без изменений, "
                    + "поэтому очистить заполненное поле этим методом нельзя. "
                    + "Аватар передаётся как avatarKey - objectKey из ответа "
                    + "POST /api/v1/file-storage/upload.",
            requestBody = @RequestBody(
                    required = true,
                    content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = UpdateProfileDTO.class))
            ),
            responses = {
                    @ApiResponse(responseCode = "200", description = "Профиль обновлён",
                            content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                                    schema = @Schema(implementation = UserDTO.class))),
                    @ApiResponse(responseCode = "400", description = "Ошибка валидации",
                            content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                                    schema = @Schema(implementation = ErrorResponse.class))),
                    @ApiResponse(responseCode = "401", description = "Неавторизованный доступ",
                            content = @Content),
                    @ApiResponse(responseCode = "409", description = "Электронная почта уже занята",
                            content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                                    schema = @Schema(implementation = ErrorResponse.class))),
                    @ApiResponse(responseCode = "500", description = "Внутренняя ошибка сервера",
                            content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                                    schema = @Schema(implementation = ErrorResponse.class)))
            }
    )
    ResponseEntity<UserDTO> updateCurrentUser(
            UpdateProfileDTO updateDTO,
            @AuthenticationPrincipal
            @Parameter(hidden = true) User currentUser);

    @Operation(
            summary = "Получить всех пользователей",
            description = "Возвращает список всех пользователей с пагинацией и фильтрацией",
            responses = {
                    @ApiResponse(responseCode = "200", description = "Успешный ответ",
                            content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                                    schema = @Schema(implementation = PageUserResponse.class))),
                    @ApiResponse(responseCode = "401", description = "Неавторизованный доступ",
                            content = @Content),
                    @ApiResponse(responseCode = "500", description = "Внутренняя ошибка сервера",
                            content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                                    schema = @Schema(implementation = ErrorResponse.class)))
            }
    )
    ResponseEntity<Page<UserDTO>> getAllUsers(
            @Parameter(description = "Номер страницы (начиная с 0)", example = "0")
            @RequestParam(defaultValue = "0") int page,

            @Parameter(description = "Размер страницы", example = "10")
            @RequestParam(defaultValue = "10") int size,

            @Parameter(
                    description = "Поле и направление сортировки (формат: поле,направление)",
                    example = "username,asc",
                    schema = @Schema(type = "string", allowableValues = {"id,asc", "id,desc", "username,asc", "username,desc", "email,asc", "email,desc", "role,asc", "role,desc", "createdAt,asc", "createdAt,desc", "updatedAt,asc", "updatedAt,desc"})
            )
            @RequestParam(defaultValue = "username,asc") String sort);

    @Operation(
            summary = "Получить пользователя по ID",
            description = "Возвращает публичную информацию о пользователе по указанному идентификатору",
            responses = {
                    @ApiResponse(responseCode = "200", description = "Успешный ответ",
                            content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                                    schema = @Schema(implementation = UserDTO.class))),
                    @ApiResponse(responseCode = "401", description = "Неавторизованный доступ",
                            content = @Content),
                    @ApiResponse(responseCode = "404", description = "Пользователь не найден",
                            content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                                    schema = @Schema(implementation = ErrorResponse.class))),
                    @ApiResponse(responseCode = "500", description = "Внутренняя ошибка сервера",
                            content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                                    schema = @Schema(implementation = ErrorResponse.class)))
            }
    )
    ResponseEntity<UserDTO> getUserById(
            @Parameter(description = "ID пользователя", example = "1")
            @PathVariable Long id);

    @Operation(
            summary = "Обновить пользователя",
            description = "Обновляет существующего пользователя. Доступно для владельца аккаунта или администратора.",
            responses = {
                    @ApiResponse(responseCode = "200", description = "Пользователь успешно обновлен",
                            content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                                    schema = @Schema(implementation = UserDTO.class))),
                    @ApiResponse(responseCode = "400", description = "Неверные данные пользователя",
                            content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                                    schema = @Schema(implementation = ErrorResponse.class))),
                    @ApiResponse(responseCode = "401", description = "Неавторизованный доступ",
                            content = @Content),
                    @ApiResponse(responseCode = "403", description = "Нет прав доступа",
                            content = @Content),
                    @ApiResponse(responseCode = "404", description = "Пользователь не найден",
                            content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                                    schema = @Schema(implementation = ErrorResponse.class))),
                    @ApiResponse(responseCode = "409", description = "Конфликт: имя пользователя или email уже заняты",
                            content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                                    schema = @Schema(implementation = ErrorResponse.class)))
            }
    )
    ResponseEntity<UserDTO> updateUser(
            @Parameter(description = "ID пользователя", example = "1")
            @PathVariable Long id,
            @RequestBody UpdateUserDTO updateDTO,
            @AuthenticationPrincipal
            @Parameter(hidden = true) User currentUser);

    @Operation(
            summary = "Удалить пользователя",
            description = "Удаляет пользователя из системы. Доступно только для администраторов.",
            responses = {
                    @ApiResponse(responseCode = "204", description = "Пользователь успешно удален",
                            content = @Content),
                    @ApiResponse(responseCode = "401", description = "Неавторизованный доступ",
                            content = @Content),
                    @ApiResponse(responseCode = "403", description = "Нет прав доступа",
                            content = @Content),
                    @ApiResponse(responseCode = "404", description = "Пользователь не найден",
                            content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                                    schema = @Schema(implementation = ErrorResponse.class))),
                    @ApiResponse(responseCode = "500", description = "Внутренняя ошибка сервера",
                            content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                                    schema = @Schema(implementation = ErrorResponse.class)))
            }
    )
    ResponseEntity<Void> deleteUser(
            @Parameter(description = "ID пользователя", example = "1")
            @PathVariable Long id);
}
