package software.pxel.learneasy.service.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import software.pxel.learneasy.api.dto.module.ModuleDetailsDTO;
import software.pxel.learneasy.api.dto.module.ModuleRequest;
import software.pxel.learneasy.api.dto.module.ModuleResponse;
import software.pxel.learneasy.api.dto.module.ModuleWithLessonList;
import software.pxel.learneasy.api.dto.userprogress.LessonProgressDTO;
import software.pxel.learneasy.api.dto.userprogress.ModuleInfoDTO;
import software.pxel.learneasy.api.dto.userprogress.ProgressDetailsDTO;
import software.pxel.learneasy.exception.BadRequestException;
import software.pxel.learneasy.exception.ResourceConflictException;
import software.pxel.learneasy.exception.ResourceNotFoundException;
import software.pxel.learneasy.mapper.ModuleMapper;
import software.pxel.learneasy.model.Course;
import software.pxel.learneasy.model.Lesson;
import software.pxel.learneasy.model.Module;
import software.pxel.learneasy.model.TestAttempt;
import software.pxel.learneasy.model.enums.CompletionStatus;
import software.pxel.learneasy.repository.CourseRepository;
import software.pxel.learneasy.repository.ModuleRepository;
import software.pxel.learneasy.repository.TestAttemptRepository;
import software.pxel.learneasy.repository.TestModelRepository;
import software.pxel.learneasy.repository.specification.ModuleSpecification;
import software.pxel.learneasy.service.ModuleService;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;

@Slf4j
@RequiredArgsConstructor
@Service
public class ModuleServiceImpl implements ModuleService {

    private final ModuleRepository moduleRepository;
    private final CourseRepository courseRepository;
    private final TestModelRepository testModelRepository;
    private final TestAttemptRepository testAttemptRepository;
    private final ModuleMapper moduleMapper;

    @Override
    @Transactional
    public ModuleResponse createModule(ModuleRequest moduleRequest) {
        log.info("Attempting to create new module for course ID: {}", moduleRequest.courseId());

        Course course = courseRepository.findById(moduleRequest.courseId())
                .orElseThrow(() -> {
                    log.warn("Course not found for new module with ID: {}", moduleRequest.courseId());
                    return new ResourceNotFoundException("Course not found with id: " + moduleRequest.courseId());
                });

        Module moduleToCreate = moduleMapper.toModule(moduleRequest);
        if (moduleToCreate == null) {
            log.warn("Module data for creation is null.");
            throw new BadRequestException("Module data cannot be null for creation.");
        }
        if (moduleToCreate.getId() != null) {
            log.warn("Attempt to create module with pre-set ID: {}. Not allowed.", moduleToCreate.getId());
            throw new BadRequestException("ID must be null for new module creation.");
        }
        moduleToCreate.setLessons(new ArrayList<>());
        moduleToCreate.setCourse(course);
        moduleToCreate.setSequenceOrder(getValidatedSequenceOrder(moduleRequest.courseId(), moduleRequest.sequenceOrder()));
        Module savedModule = moduleRepository.save(moduleToCreate);
        log.info("Successfully created module ID: {}, title: '{}'", savedModule.getId(), savedModule.getTitle());
        return moduleMapper.toModuleResponse(savedModule);
    }

    @Override
    @Transactional(readOnly = true)
    public ModuleWithLessonList getModuleById(Long id) {
        log.debug("Attempting to find module by ID: {}", id);
        if (id == null) {
            log.warn("Attempt to find module with null ID.");
            throw new BadRequestException("Module ID cannot be null.");
        }
        return moduleRepository.findByIdWithLessons(id)
                .map(moduleMapper::toModuleWithLessonListDTO).stream().findFirst().orElseThrow(() -> {
                    log.warn("Module not found with ID: {}", id);
                    return new ResourceNotFoundException("Module not found with id: " + id);
                });
    }

    @Override
    @Transactional(readOnly = true)
    public Page<ModuleResponse> getAllModules(
            String title,
            String description,
            Long courseId,
            LocalDateTime createdAfter,
            LocalDateTime createdBefore,
            Pageable pageable) {

        Specification<Module> spec = ModuleSpecification.filterSpecification(
                title,
                description,
                courseId,
                createdAfter,
                createdBefore
        );

        return moduleRepository.findAll(spec, pageable)
                .map(moduleMapper::toModuleResponse);
    }

    @Override
    @Transactional
    public ModuleResponse updateModule(Long id, ModuleRequest moduleRequest) {
        log.info("Attempting to update module with ID: {}", id);

        if (id == null) {
            log.warn("Attempt to update module with null path ID.");
            throw new BadRequestException("Module ID in path for update cannot be null.");
        }

        if (moduleRequest == null) {
            log.warn("Module data for update is null for ID: {}", id);
            throw new BadRequestException("Module data cannot be null for update.");
        }

        Course course = courseRepository.findById(moduleRequest.courseId())
                .orElseThrow(() -> {
                    log.warn("Course not found for module update with ID: {}", moduleRequest.courseId());
                    return new ResourceNotFoundException("Course not found with id: " + moduleRequest.courseId());
                });

        Module existingModule = moduleRepository.findById(id)
                .orElseThrow(() -> {
                    log.warn("Module not found for update with ID: {}", id);
                    return new ResourceNotFoundException("Module not found with id: " + id + " for update.");
                });
        existingModule.setCourse(course);
        existingModule.setTitle(moduleRequest.title());
        existingModule.setDescription(moduleRequest.description());
        existingModule.setSequenceOrder(getValidatedSequenceOrder(moduleRequest.courseId(), moduleRequest.sequenceOrder()));

        Module updatedModule = moduleRepository.save(existingModule);
        log.info("Successfully updated module ID: {}, new title: '{}'", updatedModule.getId(), updatedModule.getTitle());
        return moduleMapper.toModuleResponse(updatedModule);
    }

    @Override
    @Transactional
    public void deleteModuleById(Long id) {
        log.info("Attempting to delete module with ID: {}", id);
        if (id == null) {
            log.warn("Attempt to delete module with null ID.");
            throw new BadRequestException("Module ID for deletion cannot be null.");
        }
        moduleRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Module not found with id: " + id + ". Cannot delete."));

        moduleRepository.deleteById(id);
        log.info("Successfully deleted module with ID: {}", id);
    }

    @Override
    @Transactional(readOnly = true)
    public ModuleDetailsDTO getModuleDetailsWithProgress(Long courseId, Long moduleId, Long userId) {
        Module module = moduleRepository.findByIdWithLessons(moduleId)
                .orElseThrow(() -> new ResourceNotFoundException("Module not found with id: " + moduleId));

        if (!module.getCourse().getId().equals(courseId)) {
            throw new BadRequestException("Module " + moduleId + " does not belong to course " + courseId);
        }

        Set<Long> passedTestLessonIds = testAttemptRepository.findPassedLessonIdsByUserIdAndModuleIds(userId, Set.of(moduleId));
        Set<Long> lessonsWithTests = testModelRepository.findLessonIdsWithTestsByModuleIds(Set.of(moduleId));

        int totalLessons = module.getLessons().size();
        long completedLessonsCount = 0;

        List<LessonProgressDTO> lessonProgressDTOs = new ArrayList<>();
        boolean previousLessonCompleted = true;

        for (Lesson lesson : module.getLessons()) {
            boolean hasTest = lessonsWithTests.contains(lesson.getId());
            boolean isTestPassed = passedTestLessonIds.contains(lesson.getId());
            boolean isLessonCompleted = !hasTest || isTestPassed;

            String status;
            if (!previousLessonCompleted) {
                status = CompletionStatus.BLOCKED.toString();
            } else if (isLessonCompleted) {
                status = CompletionStatus.COMPLETED.toString();
                completedLessonsCount++;
            } else {
                status = CompletionStatus.NOT_STARTED.toString();
            }

            Integer testResult = null;
            if (hasTest) {
                testResult = testAttemptRepository.findFirstByUserIdAndTestLessonIdOrderByCreatedAtDesc(userId, lesson.getId())
                        .map(TestAttempt::getScorePercent)
                        .orElse(null);
            }

            ProgressDetailsDTO progress = new ProgressDetailsDTO(isLessonCompleted, false, isTestPassed, testResult);
            lessonProgressDTOs.add(new LessonProgressDTO(lesson.getId(), lesson.getTitle(), lesson.getSequenceOrder(), status, progress));

            previousLessonCompleted = status.equals(CompletionStatus.COMPLETED.toString());
        }

        String moduleStatus;
        boolean allTestableLessonsPassed = lessonsWithTests.isEmpty() || passedTestLessonIds.containsAll(lessonsWithTests);

        if (allTestableLessonsPassed) {
            moduleStatus = CompletionStatus.COMPLETED.toString();
        } else if (completedLessonsCount > 0) {
            moduleStatus = CompletionStatus.IN_PROGRESS.toString();
        } else {
            moduleStatus = CompletionStatus.NOT_STARTED.toString();
        }

        ModuleInfoDTO moduleInfo = new ModuleInfoDTO(
                module.getId(),
                module.getTitle(),
                module.getDescription(),
                totalLessons,
                completedLessonsCount,
                moduleStatus,
                module.getSequenceOrder()
        );

        return new ModuleDetailsDTO(moduleInfo, lessonProgressDTOs);
    }

    private Integer getValidatedSequenceOrder(Long courseId, Integer sequenceOrder) {
        List<Integer> listSequenceOrder = moduleRepository.findAllSequenceOrderByCourseId(courseId);

        if (listSequenceOrder == null || listSequenceOrder.isEmpty())
            return 0;

        Integer maxSequence = Collections.max(listSequenceOrder);
        if (listSequenceOrder.contains(sequenceOrder)) {
            log.warn("Attempt to create module with sequence order: {}. Conflict", sequenceOrder);
            throw new ResourceConflictException("Sequence order cannot be duplicated. Max sequence order: " + maxSequence);
        }
        return sequenceOrder;
    }
}
