package software.pxel.learneasy.api.dto.password;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

@Schema(description = "Запрос на сброс пароля по электронной почте")
public record ForgotPasswordRequest(
        @Schema(description = "Электронная почта пользователя", example = "user@example.com")
        @NotBlank(message = "Email не может быть пустым")
        @Email(message = "Некорректный формат email")
        String email
) {
}
