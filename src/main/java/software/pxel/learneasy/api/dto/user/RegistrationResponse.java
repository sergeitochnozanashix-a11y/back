package software.pxel.learneasy.api.dto.user;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Модель ответа для успешной инициации регистрации")
public record RegistrationResponse(
        @Schema(
                description = "Сообщение для пользователя с инструкциями",
                example = "Для завершения регистрации введите код, отправленный на ваш email."
        )
        String message,

        @Schema(
                description = "Email адрес, на который было отправлено письмо",
                example = "user@example.com"
        )
        String email
) {
}
