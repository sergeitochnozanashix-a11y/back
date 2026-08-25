package software.pxel.learneasy.feign.ai.dto;

import lombok.Builder;

import java.util.List;

@Builder
public record ChatCompletionsResponse(
        String id,
        String object,
        long created,
        String model,
        List<ChatChoice> choices,
        UsageStats usage
) {
}
