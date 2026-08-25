package software.pxel.learneasy.controller.api;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.headers.Header;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.parameters.RequestBody;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import software.pxel.learneasy.api.dto.common.ErrorResponse;
import software.pxel.learneasy.api.dto.common.MessageResponse;
import software.pxel.learneasy.api.dto.auth.AuthRequest;
import software.pxel.learneasy.api.dto.auth.AuthResponse;
import software.pxel.learneasy.api.dto.auth.JwtResponse;
import software.pxel.learneasy.api.dto.auth.RegisterRequest;
import software.pxel.learneasy.api.dto.password.ForgotPasswordRequest;
import software.pxel.learneasy.api.dto.password.ResetPasswordRequest;
import software.pxel.learneasy.api.dto.user.RegistrationResponse;
import software.pxel.learneasy.api.dto.user.ResendCodeRequest;
import software.pxel.learneasy.api.dto.user.VerificationResponse;
import software.pxel.learneasy.api.dto.user.VerifyEmailRequest;

import java.util.Map;

@Tag(name = "Authentication API", description = "API для аутентификации и регистрации пользователей")
public interface AuthApi {

    @Operation(
            summary = "Аутентификация пользователя",
            description = "Позволяет пользователю войти в систему, предоставляя имя пользователя и пароль. В случае успеха возвращает access-токен и устанавливает refresh-токен в HttpOnly куки.",
            requestBody = @RequestBody(description = "Данные для входа", required = true,
                    content = @Content(schema = @Schema(implementation = AuthRequest.class)))
    )
    @ApiResponse(
            responseCode = "200",
            description = "Успешная аутентификация",
            headers = @Header(name = "Set-Cookie", description = "Устанавливает HttpOnly куку с refresh-токеном", schema = @Schema(type = "string")),
            content = @Content(
                    mediaType = MediaType.APPLICATION_JSON_VALUE,
                    schema = @Schema(implementation = AuthResponse.class)
            )
    )
    @ApiResponse(
            responseCode = "400",
            description = "Некорректный запрос (например, ошибки валидации)",
            content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE, schema = @Schema(implementation = ErrorResponse.class))
    )
    @ApiResponse(
            responseCode = "401",
            description = "Ошибка аутентификации (неверные учетные данные)",
            content = @Content
    )
    @ApiResponse(
            responseCode = "500",
            description = "Внутренняя ошибка сервера",
            content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE, schema = @Schema(implementation = ErrorResponse.class))
    )
    ResponseEntity<AuthResponse> login(@Valid @RequestBody AuthRequest authRequest, HttpServletResponse response);

    @Operation(
            summary = "Регистрация нового пользователя",
            description = "Позволяет новому пользователю зарегистрироваться в системе. В случае успеха возвращает access-токен и устанавливает refresh-токен в HttpOnly куки.",
            requestBody = @RequestBody(description = "Данные для регистрации", required = true,
                    content = @Content(schema = @Schema(implementation = RegisterRequest.class)))
    )
    @ApiResponse(
            responseCode = "200",
            description = "Успешная регистрация",
            headers = @Header(name = "Set-Cookie", description = "Устанавливает HttpOnly куку с refresh-токеном", schema = @Schema(type = "string")),
            content = @Content(
                    mediaType = MediaType.APPLICATION_JSON_VALUE,
                    schema = @Schema(implementation = AuthResponse.class)
            )
    )
    @ApiResponse(
            responseCode = "400",
            description = "Некорректный запрос (например, ошибки валидации, некорректный email)",
            content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE, schema = @Schema(implementation = ErrorResponse.class))
    )
    @ApiResponse(
            responseCode = "409",
            description = "Конфликт (например, пользователь с таким именем или email уже существует)",
            content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE, schema = @Schema(implementation = ErrorResponse.class))
    )
    @ApiResponse(
            responseCode = "500",
            description = "Внутренняя ошибка сервера",
            content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE, schema = @Schema(implementation = ErrorResponse.class))
    )
    ResponseEntity<RegistrationResponse> register(@Valid @RequestBody RegisterRequest registerRequest);

    @Operation(
            summary = "Обновление токенов доступа",
            description = "Использует refresh-токен из куки для получения нового access-токена. В случае успеха возвращает новый access-токен и обновляет refresh-токен в куках.",
            responses = {
                    @ApiResponse(
                            responseCode = "200",
                            description = "Успешное обновление токенов",
                            headers = @Header(name = "Set-Cookie", description = "Устанавливает новую HttpOnly куку с refresh-токеном", schema = @Schema(type = "string")),
                            content = @Content(
                                    mediaType = MediaType.APPLICATION_JSON_VALUE,
                                    schema = @Schema(implementation = JwtResponse.class)
                            )
                    ),
                    @ApiResponse(
                            responseCode = "401",
                            description = "Некорректный или просроченный refresh-токен",
                            content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE, schema = @Schema(implementation = ErrorResponse.class))
                    ),
                    @ApiResponse(
                            responseCode = "500",
                            description = "Внутренняя ошибка сервера",
                            content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE, schema = @Schema(implementation = ErrorResponse.class))
                    )
            }
    )
    @PostMapping("/refresh")
    ResponseEntity<JwtResponse> refreshToken(HttpServletResponse response);

    @Operation(
            summary = "Верификация email пользователя",
            description = "Принимает email и 6-значный код верификации. В случае успеха активирует аккаунт " +
                    "пользователя (устанавливает is_verified = true), возвращает accessToken в теле ответа " +
                    "и устанавливает refreshToken в безопасный HttpOnly cookie."
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200",
                    description = "Успешная верификация. Токены сгенерированы.",
                    content = @Content(schema = @Schema(implementation = VerificationResponse.class))
            ),
            @ApiResponse(
                    responseCode = "400",
                    description = "Неверный или истекший код верификации.",
                    content = @Content(schema = @Schema(example = "{\"error\": \"Неверный или истекший код верификации.\"}"))
            ),
            @ApiResponse(
                    responseCode = "404",
                    description = "Пользователь с указанным email не найден.",
                    content = @Content(schema = @Schema(example = "{\"error\": \"Пользователь с таким email не найден.\"}"))
            )
    })
    ResponseEntity<VerificationResponse> verifyEmail(@Valid @RequestBody VerifyEmailRequest request, HttpServletResponse response);

    @Operation(
            summary = "Повторная отправка кода верификации",
            description = "Позволяет неверифицированному пользователю запросить новый код подтверждения на свой email. " +
                    "Применяется ограничение по частоте запросов: не более 5 попыток в течение 2 часов. " +
                    "После превышения лимита отправка блокируется на 2 часа."
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200",
                    description = "Новый код верификации успешно отправлен.",
                    content = @Content(schema = @Schema(example = "{\"message\": \"Новый код верификации отправлен.\"}"))
            ),
            @ApiResponse(
                    responseCode = "404",
                    description = "Неверифицированный пользователь с указанным email не найден.",
                    content = @Content(schema = @Schema(example = "{\"error\": \"Неверифицированный пользователь с таким email не найден.\"}"))
            ),
            @ApiResponse(
                    responseCode = "429",
                    description = "Превышен лимит на количество запросов.",
                    content = @Content(schema = @Schema(example = "{\"error\": \"Вы превысили лимит отправки писем. Попробуйте позже.\"}"))
            )
    })
    ResponseEntity<Map<String, String>> resendCode(@Valid @RequestBody ResendCodeRequest request);

    @Operation(
            summary = "Выход пользователя из системы",
            description = "Деактивирует refresh-токен пользователя и удаляет соответствующую куку.",
            responses = {
                    @ApiResponse(
                            responseCode = "200",
                            description = "Успешный выход из системы",
                            headers = @Header(name = "Set-Cookie", description = "Удаляет HttpOnly куку с refresh-токеном", schema = @Schema(type = "string"))
                    )
            }
    )
    @PostMapping("/logout")
    ResponseEntity<Void> logout(HttpServletResponse response);

    @Operation(
            summary = "Запрос на сброс пароля",
            description = "Инициирует процесс сброса пароля, отправляя ссылку на email.",
            responses = {
                    @ApiResponse(responseCode = "200", description = "Запрос успешно обработан",
                            content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                                    schema = @Schema(implementation = MessageResponse.class))),
                    @ApiResponse(responseCode = "400", description = "Неверный формат запроса",
                            content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                                    schema = @Schema(implementation = ErrorResponse.class)))
            }
    )
    ResponseEntity<MessageResponse> forgotPassword(@Valid @RequestBody ForgotPasswordRequest request);

    @Operation(
            summary = "Сброс пароля с использованием токена",
            description = "Устанавливает новый пароль для пользователя, используя токен сброса.",
            responses = {
                    @ApiResponse(responseCode = "200", description = "Пароль успешно изменён",
                            content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                                    schema = @Schema(implementation = MessageResponse.class))),
                    @ApiResponse(responseCode = "400", description = "Неверный токен или новый пароль не соответствует требованиям",
                            content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                                    schema = @Schema(implementation = ErrorResponse.class)))
            }
    )
    ResponseEntity<MessageResponse> resetPassword(@Valid @RequestBody ResetPasswordRequest request);
}
