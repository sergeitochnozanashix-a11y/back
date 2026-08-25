package software.pxel.learneasy.mapper;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Named;
import software.pxel.learneasy.api.dto.content.CreateBlockBaseDTO;
import software.pxel.learneasy.api.dto.content.createblock.*;
import software.pxel.learneasy.api.dto.content.properties.*;
import software.pxel.learneasy.api.dto.content.createblock.children.AdviceBlockDTO;
import software.pxel.learneasy.api.dto.content.createblock.children.ListItemBlockDTO;
import software.pxel.learneasy.api.dto.content.createblock.children.UnorderedListBlockDTO;
import software.pxel.learneasy.api.dto.lesson.LessonRequest;
import software.pxel.learneasy.api.dto.lesson.LessonWithContentList;
import software.pxel.learneasy.api.dto.lesson.LessonWithoutContentList;
import software.pxel.learneasy.model.Lesson;
import software.pxel.learneasy.model.LessonContentBlock;

import java.util.List;

@Slf4j
@Mapper(componentModel = "spring")
public abstract class LessonMapper {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Mapping(target = "id", source = "lesson.id")
    @Mapping(target = "title", source = "lesson.title")
    @Mapping(target = "description", source = "lesson.description")
    @Mapping(target = "sequenceOrder", source = "lesson.sequenceOrder")
    @Mapping(target = "testQuestions", source = "testQuestions")
    @Mapping(target = "moduleSequenceOrder", source = "moduleSequenceOrder")
    @Mapping(target = "content", source = "lesson.content")
    @Mapping(target = "contentBlocks", source = "lesson.contentBlocks", qualifiedByName = "mapContentBlocksToResponse")
    public abstract LessonWithContentList toLessonWithContentList(
            Lesson lesson,
            Integer moduleSequenceOrder,
            Integer testQuestions
    );

    @Named("mapContentBlocksToResponse")
    public List<CreateBlockBaseDTO> mapContentListToBlockBaseDTOList(List<LessonContentBlock> contentBlocks) {
        if (contentBlocks == null) {
            return List.of();
        }

        return contentBlocks.stream()
                .filter(block -> block.getParent() == null)
                .map(this::toBlockBaseDTO)
                .toList();
    }

    public CreateBlockBaseDTO toBlockBaseDTO(LessonContentBlock block) {
        if (block == null) {
            return null;
        }

        // Рекурсивный вызов для дочерних элементов
        List<CreateBlockBaseDTO> children = (block.getChildren() != null)
                ? block.getChildren().stream().map(this::toBlockBaseDTO).toList()
                : null;

        // Определяем тип блока и создаем нужный DTO
        return switch (block.getBlockType()) {
            case HEADING -> new HeadingBlockDTO(
                    block.getId(),
                    block.getBlockType(),
                    block.getContent(),
                    deserializeProperties(block.getProperties(), HeadingProperties.class)
            );
            case TEXT -> new TextBlockDTO(
                    block.getId(),
                    block.getBlockType(),
                    block.getContent(),
                    deserializeProperties(block.getProperties(), TextProperties.class)
            );
            case LINK -> new LinkBlockDTO(
                    block.getId(),
                    block.getBlockType(),
                    block.getContent(),
                    deserializeProperties(block.getProperties(), LinkProperties.class)
            );
            case CODE -> new CodeBlockDTO(
                    block.getId(),
                    block.getBlockType(),
                    block.getContent(),
                    deserializeProperties(block.getProperties(), CodeProperties.class)
            );
            case IMAGE -> new ImageBlockDTO(
                    block.getId(),
                    block.getBlockType(),
                    block.getContent(),
                    deserializeProperties(block.getProperties(), ImageProperties.class)
            );
            case ADVICE, WARNING, BOXED_TEXT -> new AdviceBlockDTO(
                    block.getId(),
                    block.getBlockType(),
                    block.getContent(),
                    deserializeProperties(block.getProperties(), CommonProperties.class),
                    children
            );
            case UNORDERED_LIST -> new UnorderedListBlockDTO(
                    block.getId(),
                    block.getBlockType(),
                    block.getContent(),
                    deserializeProperties(block.getProperties(), CommonProperties.class),
                    children
            );
            case LIST_ITEM -> new ListItemBlockDTO(
                    block.getId(),
                    block.getBlockType(),
                    block.getContent(),
                    deserializeProperties(block.getProperties(), CommonProperties.class)
            );
            default -> throw new IllegalArgumentException("Unsupported block type: " + block.getBlockType());
        };
    }

    private <T> T deserializeProperties(JsonNode properties, Class<T> clazz) {
        if (properties == null) {
            return null;
        }
        try {
            return objectMapper.treeToValue(properties, clazz);
        } catch (Exception e) {
            log.error(e.getMessage());
            return null;
        }
    }

    @Named("mapLessonsToListLessonWithoutContent")
    public abstract List<LessonWithoutContentList> mapLessonsToListLessonWithoutContent(List<Lesson> lessons);

    @Mapping(target = "content", ignore = true)
    @Mapping(target = "contentBlocks", ignore = true)
    public abstract Lesson toLesson(LessonRequest lessonRequest);

    public abstract LessonWithoutContentList toLessonWithoutContentList(Lesson lesson);
}
