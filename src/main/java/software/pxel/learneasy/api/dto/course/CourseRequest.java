package software.pxel.learneasy.api.dto.course;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

@Schema(description = "Модель запроса для создания или обновления курса")
public record CourseRequest(
        @Schema(description = "Название курса", example = "Основы Java-разработки")
        @NotBlank(message = "Название не может быть пустым")
        @Size(min = 3, max = 255, message = "Название курса должно содержать от 3 до 255 символов")
        String title,

        @Schema(description = "Описание курса", example = "Курс для начинающих, охватывающий синтаксис Java, ООП и работу с основными коллекциями.")
        @NotBlank(message = "Описание не может быть пустым")
        String description
) {
}
