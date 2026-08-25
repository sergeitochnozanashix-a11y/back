package software.pxel.learneasy.feign.ai.dto;

public record AudioTranscriptionRequest(String audioFileUrl, String model, String instructions) {
}
