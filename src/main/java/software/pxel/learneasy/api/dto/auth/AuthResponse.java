package software.pxel.learneasy.api.dto.auth;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;

@Builder
@Schema(description = "Модель ответа для успешной аутентификации или регистрации")
public record AuthResponse(
        @Schema(description = "Имя пользователя", example = "testuser") String username,
        @Schema(description = "JsonWebToken для доступа к защищенным ресурсам", example = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJzdWIiOiJ0ZXN0dXNlciIsInJvbGUiOiJVU0VSIiwiaWF0IjoxNzA0MDY3MjAwLCJleHAiOjE3MDQwNzgwMDB9.SflKxwRJSMeKKF2QT4fwpMeJf36POk6yJV_adQssw5c") String accessToken
) {
}
