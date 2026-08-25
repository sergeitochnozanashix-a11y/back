package software.pxel.learneasy.service.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import software.pxel.learneasy.api.dto.content.CreateBlockBaseDTO;
import software.pxel.learneasy.api.dto.lesson.*;
import software.pxel.learneasy.exception.BadRequestException;
import software.pxel.learneasy.exception.ResourceNotFoundException;
import software.pxel.learneasy.mapper.LessonMapper;
import software.pxel.learneasy.model.Lesson;
import software.pxel.learneasy.model.Module;
import software.pxel.learneasy.model.TestModel;
import software.pxel.learneasy.repository.LessonRepository;
import software.pxel.learneasy.repository.ModuleRepository;
import software.pxel.learneasy.repository.TestModelRepository;
import software.pxel.learneasy.service.LessonContentService;
import software.pxel.learneasy.service.LessonService;

import java.util.List;

@Slf4j
@RequiredArgsConstructor
@Service
public class LessonServiceImpl implements LessonService {

    private final LessonRepository lessonRepository;
    private final ModuleRepository moduleRepository;
    private final LessonMapper lessonMapper;
    private final TestModelRepository testModelRepository;
    private final LessonContentService lessonContentService;

    @Override
    @Transactional
    @CacheEvict(value = {"lessonListByModule"}, key = "#lessonRequest.moduleId()", allEntries = true)
    public LessonWithContentList createLesson(LessonRequest lessonRequest) {
        log.info("Attempting to create a new lesson.");
        Long moduleId = lessonRequest.moduleId();

        if (moduleId == null) {
            throw new BadRequestException("Module ID is required.");
        }

        Module module = moduleRepository.findById(moduleId)
                .orElseThrow(() -> new ResourceNotFoundException("Module not found with ID: " + moduleId));

        Lesson newLesson = lessonMapper.toLesson(lessonRequest);
        newLesson.setModule(module);

        if (newLesson.getSequenceOrder() == null) {
            int nextOrder = lessonRepository.findMaxSequenceOrderByModuleId(moduleId)
                    .orElse(-1) + 1;
            log.info("Creating a new lesson with sequence order: {}", nextOrder);
            newLesson.setSequenceOrder(nextOrder);
        }

        lessonContentService.setContentOnCreate(newLesson, lessonRequest.content(), lessonRequest.contentBlocks());

        module.getLessons().add(newLesson);

        Lesson savedLesson = lessonRepository.save(newLesson);

        log.info("Successfully created lesson with ID: {}, title: '{}'", savedLesson.getId(), savedLesson.getTitle());
        return lessonMapper.toLessonWithContentList(savedLesson, module.getSequenceOrder(), null);
    }

    @Override
    @Transactional(readOnly = true)
    public LessonWithContentList getLessonById(Long id) {
        log.debug("Attempting to find lesson by ID: {}", id);
        if (id == null) {
            log.warn("Attempt to find lesson with null ID.");
            throw new BadRequestException("Lesson ID cannot be null.");
        }
        return lessonRepository.findByIdWithContentBlock(id)
                .map(lesson -> {
                    Integer moduleSequenceOrder =
                            moduleRepository.findSequenceOrderById(lesson.getModule().getId());

                    Integer testQuestions = null;
                    if (testModelRepository.existsByLessonId(id)) {
                        int cnt = testModelRepository.countQuestionsByLessonId(id);
                        testQuestions = cnt == 0 ? null : cnt;
                    }

                    return lessonMapper.toLessonWithContentList(lesson, moduleSequenceOrder, testQuestions);
                })
                .orElseThrow(() -> {
                    log.warn("Lesson not found with ID: {}", id);
                    return new ResourceNotFoundException("Lesson not found with id: " + id);
                });
    }

    @Override
    @Transactional(readOnly = true)
    public Page<LessonWithoutContentList> getLessonsByModuleId(
            Long moduleId,
            Pageable pageable) {
        log.info("Attempting to retrieve all lessons.");
        return lessonRepository.findAllByModuleIdWithFilters(moduleId, pageable)
                .map(lessonMapper::toLessonWithoutContentList);
    }

    @Transactional
    @Override
    public LessonWithContentList updateLesson(Long id, UpdateLessonRequest lessonRequest) {
        log.info("Attempting to update lesson with ID: {}", id);
        if (id == null) {
            log.warn("Attempt to update lesson with null ID.");
            throw new BadRequestException("Lesson ID cannot be null.");
        }

        Lesson existingLesson = lessonRepository.findById(id)
                .orElseThrow(() -> {
                    log.warn("Lesson not found for update with ID: {}", id);
                    return new ResourceNotFoundException("Lesson not found with id: " + id + " for update.");
                });

        updateLessonMetadata(existingLesson, lessonRequest);
        lessonContentService.setContentOnUpdate(existingLesson, lessonRequest.content(), lessonRequest.contentBlocks());

        Lesson updatedLesson = lessonRepository.save(existingLesson);
        log.info("Successfully updated lesson ID: {}, new title: '{}'", updatedLesson.getId(), updatedLesson.getTitle());

        Integer moduleSequenceOrder = moduleRepository.findSequenceOrderById(lessonRequest.moduleId());
        return lessonMapper.toLessonWithContentList(updatedLesson, moduleSequenceOrder, null);
    }

    private void updateLessonMetadata(Lesson existing, UpdateLessonRequest request) {
        if (request.title() != null) {
            existing.setTitle(request.title());
        }
        if (request.description() != null) {
            existing.setDescription(request.description());
        }
        if (request.sequenceOrder() != null) {
            existing.setSequenceOrder(request.sequenceOrder());
        }
    }

    @Override
    @Transactional
    public void deleteLessonById(Long id) {
        log.info("Attempting to delete lesson with ID: {}", id);
        if (id == null) {
            log.warn("Attempt to delete lesson with null ID.");
            throw new BadRequestException("Lesson ID for deletion cannot be null.");
        }
        if (!lessonRepository.existsById(id)) {
            log.warn("Lesson not found for deletion with ID: {}", id);
            throw new ResourceNotFoundException("Lesson not found with id: " + id + ". Cannot delete.");
        }
        lessonRepository.deleteById(id);
        log.info("Successfully deleted lesson with ID: {}", id);
    }

    @Override
    @Transactional
    public LessonWithContentList updateLessonContent(Long lessonId, List<CreateBlockBaseDTO> requestBlocks) {
        Lesson lesson = lessonRepository.findById(lessonId)
                .orElseThrow(() -> new ResourceNotFoundException("Lesson not found with id: " + lessonId));

        lessonContentService.updateJsonContent(lesson, requestBlocks);
        Lesson savedLesson = lessonRepository.save(lesson);

        Integer moduleSequenceOrder = moduleRepository.findSequenceOrderById(savedLesson.getModule().getId());
        return lessonMapper.toLessonWithContentList(savedLesson, moduleSequenceOrder, null);
    }

    @Override
    @Transactional
    public LessonWithContentList updateLessonMarkdownContent(Long lessonId, String markdownContent) {
        Lesson lesson = lessonRepository.findById(lessonId)
                .orElseThrow(() -> new ResourceNotFoundException("Lesson not found with id: " + lessonId));

        lessonContentService.updateMarkdownContent(lesson, markdownContent);
        Lesson savedLesson = lessonRepository.save(lesson);

        Integer moduleSequenceOrder = moduleRepository.findSequenceOrderById(savedLesson.getModule().getId());
        return lessonMapper.toLessonWithContentList(savedLesson, moduleSequenceOrder, null);
    }

    @Override
    @Transactional(readOnly = true)
    public LessonSequenceDTO getLessonSequenceOrders(Long lessonId) {
        if (lessonId == null) {
            throw new BadRequestException("Lesson ID cannot be null.");
        }

        Lesson lesson = lessonRepository.findById(lessonId)
                .orElseThrow(() -> new ResourceNotFoundException("Lesson not found with id: " + lessonId));

        Integer lessonSeq = lesson.getSequenceOrder();
        Long moduleId = lesson.getModule().getId();
        Integer moduleSeq = moduleRepository.findSequenceOrderById(moduleId);

        Long testId = testModelRepository.findByLessonId(lessonId)
                .map(TestModel::getId)
                .orElse(null);

        return new LessonSequenceDTO(lessonSeq, moduleSeq, testId);
    }
}
