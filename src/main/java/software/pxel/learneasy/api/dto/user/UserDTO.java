package software.pxel.learneasy.api.dto.user;

import io.swagger.v3.oas.annotations.media.Schema;
import software.pxel.learneasy.model.enums.Gender;
import software.pxel.learneasy.model.enums.UserRole;

import java.time.Instant;
import java.time.LocalDate;

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
        Instant createdAt,

        // --- Профильные поля. Все могут быть null: профиль заполняется постепенно,
        // а до первого сохранения строки в user_profiles вообще нет.
        @Schema(description = "Имя", example = "Иван", nullable = true)
        String firstName,
        @Schema(description = "Фамилия", example = "Иванов", nullable = true)
        String lastName,
        @Schema(description = "Пол", example = "MALE", nullable = true)
        Gender gender,
        @Schema(description = "Номер телефона в произвольном формате", example = "+7 999 123-45-67", nullable = true)
        String phone,
        @Schema(description = "Адрес", example = "ул. Ленина, д. 1, кв. 2", nullable = true)
        String address,
        @Schema(description = "Дата рождения", example = "1990-05-17", nullable = true)
        LocalDate birthDate,
        @Schema(description = "Страна", example = "Россия", nullable = true)
        String country,
        @Schema(description = "Город", example = "Москва", nullable = true)
        String city,
        @Schema(
                description = "Ключ файла аватара в S3. Изображение запрашивается как "
                        + "GET /api/v1/file-storage/download/{avatarKey}",
                example = "a1b2c3d4-e5f6-7890-abcd-ef1234567890.jpg",
                nullable = true
        )
        String avatarKey
) {
}
