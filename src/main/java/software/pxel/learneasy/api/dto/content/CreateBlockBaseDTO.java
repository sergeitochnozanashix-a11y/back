package software.pxel.learneasy.api.dto.content;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import software.pxel.learneasy.api.dto.content.createblock.*;
import software.pxel.learneasy.api.dto.content.createblock.children.*;
import software.pxel.learneasy.api.dto.content.properties.CommonProperties;
import software.pxel.learneasy.model.enums.ContentBlockType;

import java.util.List;

@JsonTypeInfo(
        use = JsonTypeInfo.Id.NAME,
        include = JsonTypeInfo.As.EXISTING_PROPERTY,
        property = "blockType",
        visible = true
)
@JsonSubTypes({
        @JsonSubTypes.Type(value = HeadingBlockDTO.class, name = "HEADING"),
        @JsonSubTypes.Type(value = TextBlockDTO.class, name = "TEXT"),
        @JsonSubTypes.Type(value = LinkBlockDTO.class, name = "LINK"),
        @JsonSubTypes.Type(value = CodeBlockDTO.class, name = "CODE"),
        @JsonSubTypes.Type(value = ImageBlockDTO.class, name = "IMAGE"),
        @JsonSubTypes.Type(value = VideoBlockDTO.class, name = "VIDEO"),
        @JsonSubTypes.Type(value = AdviceBlockDTO.class, name = "ADVICE"),
        @JsonSubTypes.Type(value = WarningBlockDTO.class, name = "WARNING"),
        @JsonSubTypes.Type(value = BoxedTextBlockDTO.class, name = "BOXED_TEXT"),
        @JsonSubTypes.Type(value = UnorderedListBlockDTO.class, name = "UNORDERED_LIST"),
        @JsonSubTypes.Type(value = ListItemBlockDTO.class, name = "LIST_ITEM")
})
public interface CreateBlockBaseDTO {
    @Positive
    Long id();

    @NotNull(message = "Тип блока не может быть пустым")
    ContentBlockType blockType();

    CommonProperties properties();

    String content();

    Boolean isHasChildren();

    List<CreateBlockBaseDTO> children();

}