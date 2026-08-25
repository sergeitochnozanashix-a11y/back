package software.pxel.learneasy.api.dto.common;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "DTO для сообщений ответа API")
public record MessageResponse(
        @Schema(description = "Сообщение от сервера", example = "Операция успешно выполнена.")
        String message
) {
}
