package software.pxel.learneasy.api.dto.user;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Size;

@Schema(description = "DTO для обновления информации о пользователе")
public record UpdateUserDTO(
        @Schema(description = "Новое имя пользователя", example = "new_username")
        @Size(min = 3, max = 50, message = "Имя пользователя должно содержать от 3 до 50 символов")
        String username,

        @Schema(description = "Новая электронная почта", example = "new_email@example.com")
        @Email(message = "Электронная почта должна быть валидной")
        @Size(max = 255, message = "Электронная почта не может быть длиннее 255 символов")
        String email
) {
}
