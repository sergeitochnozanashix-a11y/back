package software.pxel.learneasy.api.dto.lesson;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import software.pxel.learneasy.api.dto.content.CreateBlockBaseDTO;

import java.util.List;

@Schema(description = "Модель запроса для создания нового урока (/lesson)")
public record LessonRequest(
        @Schema(description = "Уникальный идентификатор модуля для привязки", example = "1")
        @NotNull(message = "Идентификатор не может быть null")
        Long moduleId,

        @Schema(description = "Название урока", example = "Java Core")
        @NotBlank(message = "Название не может быть пустым")
        @Size(min = 3, max = 255, message = "Название урока должно содержать от 3 до 255 символов")
        String title,

        @Schema(description = "Описание урока", example = "Урок содержит основы ООП, работу с потоками, перегрузкой методов и т.д.")
        @NotBlank(message = "Описание не может быть пустым")
        String description,

        @Schema(description = "Содержимое урока в формате Markdown. Если указано, contentBlocks игнорируется")
        String content,

        @Schema(description = "Внутренние блоки контента (старый формат, JSON)")
        @Valid
        List<CreateBlockBaseDTO> contentBlocks
) {
}
