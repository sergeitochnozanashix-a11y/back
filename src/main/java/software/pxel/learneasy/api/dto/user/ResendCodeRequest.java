package software.pxel.learneasy.api.dto.user;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

@Schema(description = "Запрос на повторную отправку кода верификации")
public record ResendCodeRequest(
        @NotBlank
        @Email
        @Schema(
                description = "Email адрес пользователя, который еще не прошел верификацию",
                example = "user@example.com",
                requiredMode = Schema.RequiredMode.REQUIRED
        )
        String email
) {
}
