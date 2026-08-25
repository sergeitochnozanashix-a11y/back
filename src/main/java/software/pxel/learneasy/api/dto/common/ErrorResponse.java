package software.pxel.learneasy.api.dto.common;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Модель ответа в случае ошибки")
public record ErrorResponse(
        @Schema(description = "Сообщение об ошибке", example = "User not found") String message,
        @Schema(description = "HTTP код ответа", example = "404") Integer httpCode,
        @Schema(description = "Временная метка ошибки", example = "2023-01-01T12:00:00.000Z") String timestamp
) {
    public ErrorResponse(String message, Integer httpCode) {
        this(message, httpCode, java.time.Instant.now().toString());
    }
}
