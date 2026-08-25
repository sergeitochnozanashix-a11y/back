package software.pxel.learneasy.api.dto.user;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

@Schema(description = "Запрос для верификации email с помощью кода")
public record VerifyEmailRequest(
        @NotBlank
        @Email
        @Schema(
                description = "Email адрес пользователя для верификации",
                example = "user@example.com",
                requiredMode = Schema.RequiredMode.REQUIRED
        )
        String email,

        @NotBlank
        @Schema(
                description = "6-значный код верификации, полученный по email",
                example = "123456",
                requiredMode = Schema.RequiredMode.REQUIRED
        )
        String code
) {
}
