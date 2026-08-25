package software.pxel.learneasy.api.dto.lesson;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import software.pxel.learneasy.api.dto.content.CreateBlockBaseDTO;

import java.util.List;

public record LessonContentUpdateRequest(
        @Valid @NotEmpty(message = "Контент урока не может быть пустым")
        List<CreateBlockBaseDTO> contentBlocks
) {
    public List<CreateBlockBaseDTO> getContentBlocks() {
        return contentBlocks;
    }
}
