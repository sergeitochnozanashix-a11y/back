package software.pxel.learneasy.feign.whisper.dto;

public record ErrorResponse(String message, Integer httpCode, String timestamp) {
}
