package software.pxel.learneasy.api.dto.auth;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

@Schema(description = "Модель запроса для регистрации нового пользователя (/register)")
public record RegisterRequest(
        @Schema(description = "Желаемый логин пользователя", example = "newuser")
        @NotBlank(message = "Имя пользователя обязательно для заполнения")
        @Size(min = 3, max = 50, message = "Имя пользователя должно содержать от 3 до 50 символов")
        String username,

        @Schema(description = "Email пользователя", example = "newuser@example.com")
        @NotBlank(message = "Email обязателен для заполнения")
        @Email(message = "Email должен быть корректным адресом электронной почты")
        String email,

        @Schema(description = "Пароль пользователя", example = "strongPassword123")
        @NotBlank(message = "Пароль обязателен для заполнения")
        @Size(min = 6, max = 100, message = "Пароль должен содержать от 6 до 100 символов")
        String password
) {
}
