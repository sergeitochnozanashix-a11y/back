package software.pxel.learneasy.api.dto.content.createblock.children;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import software.pxel.learneasy.api.dto.content.CreateBlockBaseDTO;
import software.pxel.learneasy.api.dto.content.properties.CommonProperties;
import software.pxel.learneasy.model.enums.ContentBlockType;

import java.util.List;

public record UnorderedListBlockDTO(
        @Positive
        Long id,
        @JsonProperty("blockType") @NotNull ContentBlockType blockType,
        @NotBlank(message = "Элемент списка не может быть пустым") String content,
        @Valid CommonProperties properties,
        @Valid @NotEmpty(message = "Список должен содержать хотя бы один элемент") List<CreateBlockBaseDTO> children
) implements CreateBlockBaseDTO {
    @Override
    @JsonIgnore
    public Boolean isHasChildren() {
        return true;
    }
}
