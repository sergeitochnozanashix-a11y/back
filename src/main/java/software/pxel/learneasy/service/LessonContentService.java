package software.pxel.learneasy.service;

import software.pxel.learneasy.api.dto.content.CreateBlockBaseDTO;
import software.pxel.learneasy.model.Lesson;

import java.util.List;

public interface LessonContentService {

    void setContentOnCreate(Lesson lesson, String markdownContent, List<CreateBlockBaseDTO> contentBlocks);

    void setContentOnUpdate(Lesson lesson, String markdownContent, List<CreateBlockBaseDTO> contentBlocks);

    void updateJsonContent(Lesson lesson, List<CreateBlockBaseDTO> contentBlocks);

    void updateMarkdownContent(Lesson lesson, String markdownContent);
}
