package software.pxel.learneasy.api.dto.auth;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

@Schema(description = "Модель запроса для аутентификации (/login)")
public record AuthRequest(
        @Schema(description = "Логин пользователя", example = "testuser")
        @NotBlank(message = "Логин не может быть пустым")
        @Size(min = 3, max = 50, message = "Логин должен содержать от 3 до 50 символов")
        String username,

        @Schema(description = "Пароль пользователя", example = "password")
        @NotBlank(message = "Пароль не может быть пустым")
        @Size(min = 6, max = 100, message = "Пароль должен содержать от 6 до 100 символов")
        String password
) {
}
