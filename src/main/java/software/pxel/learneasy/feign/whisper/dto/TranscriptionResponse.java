package software.pxel.learneasy.feign.whisper.dto;

public record TranscriptionResponse(String text, long durationMs) {
}
