package software.pxel.learneasy.api.dto.admin;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Данные об одном пользователе для таблицы активности")
public record UserTable(
        @Schema(description = "ID пользователя", example = "123456")
        Long id,

        @Schema(description = "Полное имя пользователя", example = "Петров Петр Петрович")
        String fullName,

        @Schema(description = "Общий прогресс прохождения курса в процентах", example = "70%")
        String progress,

        @Schema(description = "Название текущего (следующего не пройденного) модуля", example = "1. Введение")
        String currentModule,

        @Schema(description = "Порядковый номер текущего (следующего не пройденного) урока в модуле", example = "2")
        int currentLesson
) {
}
