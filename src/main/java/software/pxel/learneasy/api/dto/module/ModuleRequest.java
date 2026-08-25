package software.pxel.learneasy.api.dto.module;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

@Schema(description = "Модель запроса для создания или обновления модуля")
public record ModuleRequest(
        @Schema(description = "Название модуля", example = "Java Core")
        @NotBlank(message = "Название не может быть пустым")
        @Size(min = 3, max = 255, message = "Название модуля должно содержать от 3 до 255 символов")
        String title,

        @Schema(description = "Описание модуля", example = "Модуль содержит основы ООП, работу с потоками, перегрузкой методов и т.д.")
        @NotBlank(message = "Описание не может быть пустым")
        String description,

        @Schema(description = "ID курса, к которому принадлежит модуль", example = "1")
        @NotNull(message = "ID курса не может быть пустым")
        @Positive(message = "ID курса должен быть положительным числом")
        Long courseId,

        @Schema(description = "Порядковый номер модуля в курсе", example = "1")
        @NotNull(message = "Порядковый номер модуля в курсе не может быть пустым")
        @Positive(message = "Порядковый номер модуля в курсе должен быть положительным числом")
        Integer sequenceOrder
) {
}
