package software.pxel.learneasy.api.dto.content.createblock;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import software.pxel.learneasy.api.dto.content.CreateBlockBaseDTO;
import software.pxel.learneasy.api.dto.content.properties.CommonProperties;
import software.pxel.learneasy.model.enums.ContentBlockType;

import java.util.List;

public record VideoBlockDTO(
        @Positive Long id,
        @JsonProperty("blockType") @NotNull
        ContentBlockType blockType,
        @NotBlank @NotNull String content,
        @NotBlank(message = "Значение ключа в s3 не может быть пустым")
        String fileUrl,
        @Valid CommonProperties properties
) implements CreateBlockBaseDTO {

    @Override
    @JsonIgnore
    public Boolean isHasChildren() {
        return false;
    }

    @Override
    public List<CreateBlockBaseDTO> children() {
        throw new UnsupportedOperationException("You are trying to access children for block that can't have children");
    }
}
