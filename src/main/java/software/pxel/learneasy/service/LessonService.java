package software.pxel.learneasy.service;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import software.pxel.learneasy.api.dto.content.CreateBlockBaseDTO;
import software.pxel.learneasy.api.dto.lesson.*;

import java.util.List;

public interface LessonService {
    LessonWithContentList createLesson(LessonRequest lessonRequest);

    LessonWithContentList getLessonById(Long id);

    Page<LessonWithoutContentList> getLessonsByModuleId(Long moduleId, Pageable pageable);

    LessonWithContentList updateLesson(Long id, UpdateLessonRequest lessonUpdates);

    void deleteLessonById(Long id);

    LessonWithContentList updateLessonContent(Long lessonId, List<CreateBlockBaseDTO> requestBlocks);

    LessonWithContentList updateLessonMarkdownContent(Long lessonId, String markdownContent);

    LessonSequenceDTO getLessonSequenceOrders(Long lessonId);
}
