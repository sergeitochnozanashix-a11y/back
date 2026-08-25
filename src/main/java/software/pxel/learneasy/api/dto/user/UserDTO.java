package software.pxel.learneasy.api.dto.user;

import io.swagger.v3.oas.annotations.media.Schema;
import software.pxel.learneasy.model.enums.UserRole;

import java.time.Instant;

@Schema(description = "DTO для публичного отображения информации о пользователе")
public record UserDTO(
        @Schema(description = "Уникальный идентификатор пользователя", example = "1")
        Long id,
        @Schema(description = "Имя пользователя", example = "example_user")
        String username,
        @Schema(description = "Электронная почта пользователя", example = "user@example.com")
        String email,
        @Schema(description = "Роль пользователя", example = "USER")
        UserRole role,
        @Schema(description = "Дата и время создания пользователя", example = "2024-08-10T10:00:00Z")
        Instant createdAt
) {
}
