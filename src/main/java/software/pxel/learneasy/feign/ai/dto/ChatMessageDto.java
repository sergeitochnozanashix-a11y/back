package software.pxel.learneasy.feign.ai.dto;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;
import lombok.Builder;
import software.pxel.learneasy.api.dto.chat.AttachmentDto;

import java.util.List;

@Builder
public record ChatMessageDto(Role role, String content, List<AttachmentDto> attachments, String voiceTranscript) {

    public enum Role {
        SYSTEM,
        USER,
        ASSISTANT;

        @JsonCreator
        public static Role fromString(String value) {
            if (value == null) return null;
            return Role.valueOf(value.toUpperCase());
        }

        @JsonValue
        public String toValue() {
            return this.name().toLowerCase();
        }
    }
}
