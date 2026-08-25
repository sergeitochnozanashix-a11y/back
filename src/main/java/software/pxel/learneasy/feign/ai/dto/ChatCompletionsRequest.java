package software.pxel.learneasy.feign.ai.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;

import java.util.List;

@Builder
public record ChatCompletionsRequest(String model, @JsonProperty("messages") List<ChatMessageDto> messages) {
}
