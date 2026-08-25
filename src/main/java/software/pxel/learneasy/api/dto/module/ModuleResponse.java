package software.pxel.learneasy.api.dto.module;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Модель ответа для модуля")
public record ModuleResponse(
        @Schema(description = "Уникальный идентификатор модуля", example = "1")
        long id,

        @Schema(description = "Название модуля", example = "Java Core")
        String title,

        @Schema(description = "Описание модуля", example = "Урок содержит основы ООП, работу с потоками, перегрузкой методов и т.д.")
        String description,

        @Schema(description = "ID курса, к которому принадлежит модуль", example = "1")
        Long courseId,

        @Schema(description = "Порядковый номер модуля в курсе", example = "1")
        Integer sequenceOrder
) {
}
