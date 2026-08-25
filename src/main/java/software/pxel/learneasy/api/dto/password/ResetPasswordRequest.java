package software.pxel.learneasy.api.dto.password;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

@Schema(description = "Запрос на установку нового пароля с токеном")
public record ResetPasswordRequest(
        @Schema(description = "Токен сброса пароля", example = "a1b2c3d4-e5f6-7890-g1h2-i3j4k5l6m7n8")
        @NotBlank(message = "Токен не может быть пустым")
        String token,

        @Schema(description = "Новый пароль", example = "MyNewSecurePassword123!")
        @NotBlank(message = "Новый пароль не может быть пустым")
        @Size(min = 8, message = "Пароль должен содержать минимум 8 символов")
        String newPassword
) {
}
