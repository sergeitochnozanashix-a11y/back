package software.pxel.learneasy.service.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import software.pxel.learneasy.api.dto.content.CreateBlockBaseDTO;
import software.pxel.learneasy.model.Lesson;
import software.pxel.learneasy.model.LessonContentBlock;
import software.pxel.learneasy.model.enums.ContentBlockType;
import software.pxel.learneasy.service.LessonContentService;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

@Service
@RequiredArgsConstructor
public class LessonContentServiceImpl implements LessonContentService {

    private final ObjectMapper objectMapper;

    @Override
    public void setContentOnCreate(Lesson lesson, String markdownContent, List<CreateBlockBaseDTO> contentBlocks) {
        if (StringUtils.hasText(markdownContent)) {
            lesson.setContent(markdownContent);
            lesson.setContentBlocks(new ArrayList<>());
            return;
        }

        if (contentBlocks != null && !contentBlocks.isEmpty()) {
            List<LessonContentBlock> blocks = processContentBlocks(contentBlocks, lesson, null);
            lesson.setContentBlocks(blocks);
            lesson.setContent(null);
        }
    }

    @Override
    public void setContentOnUpdate(Lesson lesson, String markdownContent, List<CreateBlockBaseDTO> contentBlocks) {
        if (StringUtils.hasText(markdownContent)) {
            updateMarkdownContent(lesson, markdownContent);
            return;
        }

        if (contentBlocks != null) {
            updateJsonContent(lesson, contentBlocks);
        }
    }

    @Override
    public void updateJsonContent(Lesson lesson, List<CreateBlockBaseDTO> contentBlocks) {
        lesson.getContentBlocks().clear();
        List<LessonContentBlock> newBlocks = processContentBlocks(contentBlocks, lesson, null);
        lesson.getContentBlocks().addAll(newBlocks);
        lesson.setContent(null);
    }

    @Override
    public void updateMarkdownContent(Lesson lesson, String markdownContent) {
        lesson.setContent(markdownContent);
        lesson.getContentBlocks().clear();
    }

    private List<LessonContentBlock> processContentBlocks(List<CreateBlockBaseDTO> dtos, Lesson lesson, LessonContentBlock parent) {
        if (dtos == null) {
            return new ArrayList<>();
        }

        List<LessonContentBlock> blocks = new ArrayList<>();
        for (int i = 0; i < dtos.size(); i++) {
            CreateBlockBaseDTO dto = dtos.get(i);
            LessonContentBlock block = new LessonContentBlock();
            block.setLesson(lesson);
            block.setParent(parent);
            block.setSequenceOrder(i);

            mapDtoToEntity(dto, block);

            if (Boolean.TRUE.equals(dto.isHasChildren()) && dto.children() != null) {
                List<LessonContentBlock> children = processContentBlocks(dto.children(), lesson, block);
                block.setChildren(children);
            }

            blocks.add(block);
        }
        return blocks;
    }

    private void mapDtoToEntity(CreateBlockBaseDTO dto, LessonContentBlock block) {
        block.setBlockType(dto.blockType());
        block.setContent(dto.content());

        if (dto.properties() != null) {
            block.setProperties(objectMapper.valueToTree(dto.properties()));
        } else {
            block.setProperties(null);
        }

        if (Objects.requireNonNull(dto.blockType()) == ContentBlockType.IMAGE) {
            block.setAltText(null);
        }
    }
}
