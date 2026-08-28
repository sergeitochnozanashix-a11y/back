package software.pxel.learneasy.api.dto.user;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.PastOrPresent;
import jakarta.validation.constraints.Size;
import software.pxel.learneasy.model.enums.Gender;

import java.time.LocalDate;

/**
 * Тело PATCH /api/v1/users/me. Все поля необязательные, {@code null} означает
 * «не менять» - поэтому очистить уже заполненное поле через этот контракт
 * нельзя, только перезаписать непустым значением.
 * <p>
 * Логин сюда намеренно не входит: он служит subject'ом JWT, и его смена
 * обесценила бы текущий токен пользователя прямо в момент сохранения формы.
 */
@Schema(description = "DTO для обновления собственного профиля. Незаданные поля остаются без изменений")
public record UpdateProfileDTO(
        @Schema(description = "Новая электронная почта", example = "new_email@example.com", nullable = true)
        @Email(message = "Электронная почта должна быть валидной")
        @Size(max = 255, message = "Электронная почта не может быть длиннее 255 символов")
        String email,

        @Schema(description = "Имя", example = "Иван", nullable = true)
        @Size(max = 100, message = "Имя не может быть длиннее 100 символов")
        String firstName,

        @Schema(description = "Фамилия", example = "Иванов", nullable = true)
        @Size(max = 100, message = "Фамилия не может быть длиннее 100 символов")
        String lastName,

        @Schema(description = "Пол", example = "MALE", nullable = true)
        Gender gender,

        @Schema(description = "Номер телефона в произвольном формате", example = "+7 999 123-45-67", nullable = true)
        @Size(max = 32, message = "Номер телефона не может быть длиннее 32 символов")
        String phone,

        @Schema(description = "Адрес", example = "ул. Ленина, д. 1, кв. 2", nullable = true)
        @Size(max = 255, message = "Адрес не может быть длиннее 255 символов")
        String address,

        @Schema(description = "Дата рождения, не может быть в будущем", example = "1990-05-17", nullable = true)
        @PastOrPresent(message = "Дата рождения не может быть в будущем")
        LocalDate birthDate,

        @Schema(description = "Страна", example = "Россия", nullable = true)
        @Size(max = 100, message = "Название страны не может быть длиннее 100 символов")
        String country,

        @Schema(description = "Город", example = "Москва", nullable = true)
        @Size(max = 100, message = "Название города не может быть длиннее 100 символов")
        String city,

        @Schema(
                description = "Ключ файла аватара - objectKey из ответа POST /api/v1/file-storage/upload",
                example = "a1b2c3d4-e5f6-7890-abcd-ef1234567890.jpg",
                nullable = true
        )
        @Size(max = 512, message = "Ключ файла не может быть длиннее 512 символов")
        String avatarKey
) {
}
