package software.pxel.learneasy.feign.ai.dto;

public record UsageStats(int promptTokens, int completionTokens, int totalTokens) {
}
