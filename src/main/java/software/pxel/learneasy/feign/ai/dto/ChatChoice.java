package software.pxel.learneasy.feign.ai.dto;

public record ChatChoice(int index, ChatMessageDto message, String finishReason) {
}
