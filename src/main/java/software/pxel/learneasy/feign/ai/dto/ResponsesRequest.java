package software.pxel.learneasy.feign.ai.dto;

public record ResponsesRequest(String model, String instructions, Object input) {
}
