package software.pxel.learneasy.api.dto.userprogress;


import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;

@Builder
@Schema(description = "Модель транспортировки данных для передачи информации о прохождении модулей")
public record ModuleProgressDTO(
        @Schema(description = "Название модуля", example = "101")
        String moduleTitle,
        @Schema(description = "Статус прохождения модуля(Прошел/Не прошел)", example = "true")
        boolean isCompleted
) {
}
