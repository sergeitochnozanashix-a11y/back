package software.pxel.learneasy.service.impl;

import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import software.pxel.learneasy.api.dto.test.ExamDTO;
import software.pxel.learneasy.api.dto.test.LastResultDTO;
import software.pxel.learneasy.api.dto.test.TestRequest;
import software.pxel.learneasy.api.dto.test.TestResponse;
import software.pxel.learneasy.exception.ResourceConflictException;
import software.pxel.learneasy.exception.ResourceNotFoundException;
import software.pxel.learneasy.mapper.TestModelMapper;
import software.pxel.learneasy.model.Lesson;
import software.pxel.learneasy.model.Module;
import software.pxel.learneasy.model.TestAttempt;
import software.pxel.learneasy.model.TestModel;
import software.pxel.learneasy.model.enums.AnswerEvaluation;
import software.pxel.learneasy.model.enums.ExamStatus;
import software.pxel.learneasy.model.enums.TestType;
import software.pxel.learneasy.repository.LessonRepository;
import software.pxel.learneasy.repository.TestAnswerRepository;
import software.pxel.learneasy.repository.TestAttemptRepository;
import software.pxel.learneasy.repository.TestModelRepository;
import software.pxel.learneasy.repository.projection.TestModelWithQuestionsCount;
import software.pxel.learneasy.service.QuestionService;
import software.pxel.learneasy.service.TestService;

import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class TestServiceImpl implements TestService {

    private final TestModelRepository testModelRepository;
    private final LessonRepository lessonRepository;
    private final TestAttemptRepository attemptRepository;
    private final TestModelMapper testModelMapper;
    private final QuestionService questionService;
    private final TestAnswerRepository testAnswerRepository;

    @Override
    @Transactional
    public TestResponse createTest(TestRequest testRequest) {
        if (testModelRepository.existsByLessonIdAndModuleId(testRequest.lessonId(), testRequest.moduleId())) {
            throw new ResourceConflictException("Test for lesson ID " + testRequest.lessonId() + " and module ID " + testRequest.moduleId() + " already exists.");
        }

        TestModel testModel;

        if (testRequest.lessonId() != null) {
            Lesson lesson = findLessonById(testRequest.lessonId());
            Module module = lesson.getModule();
            testModel = testModelMapper.toLessonTestModel(testRequest, lesson, module);
        } else {
            // Логика для экзаменов на модуль (если потребуется)
            throw new UnsupportedOperationException("Module-level exams are not fully supported for creation this way.");
        }

        TestModel persistedTestModel = testModelRepository.save(testModel);
        questionService.replaceQuestionsForTest(persistedTestModel, testRequest.questions());
        persistedTestModel = testModelRepository.saveAndFlush(persistedTestModel);

        return testModelMapper.toTestResponse(persistedTestModel);
    }

    @Override
    @Transactional(readOnly = true)
    public TestResponse getTestById(Long testId) {
        return testModelRepository.findByIdWithQuestions(testId)
                .map(testModelMapper::toTestResponse)
                .orElseThrow(() -> new ResourceNotFoundException("Test not found with id: " + testId));
    }

    @Override
    @Transactional(readOnly = true)
    public TestResponse getTestByLessonId(Long lessonId) {
        return testModelRepository.findByLessonIdWithQuestions(lessonId)
                .map(testModelMapper::toTestResponse)
                .orElseThrow(() -> new ResourceNotFoundException("Test not found for lesson id: " + lessonId));
    }


    @Override
    @Transactional
    public TestResponse updateTest(Long testId, TestRequest testRequest) {
        TestModel existingTestModel = findTestByIdWithQuestions(testId);

        updateTestLesson(existingTestModel, testRequest.lessonId());
        testModelMapper.updateTestFromRequest(testRequest, existingTestModel);
        questionService.replaceQuestionsForTest(existingTestModel, testRequest.questions());

        existingTestModel = testModelRepository.saveAndFlush(existingTestModel);

        return testModelMapper.toTestResponse(existingTestModel);
    }

    @Override
    @Transactional
    public void deleteTest(Long testId) {
        if (!testModelRepository.existsById(testId)) {
            throw new ResourceNotFoundException("Test not found with id: " + testId);
        }
        testModelRepository.deleteById(testId);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<ExamDTO> getExamsByCourse(Long courseId, Long userId, Pageable pageable) {
        Page<TestModelWithQuestionsCount> examsPage = testModelRepository.findByCourseIdAndTestType(courseId, TestType.MODULE_EXAM, pageable);

        List<Long> moduleIdsOnPage = examsPage.getContent().stream()
                .map(TestModelWithQuestionsCount::getModuleId)
                .distinct()
                .toList();

        Set<Long> passedLessonIds = attemptRepository.findPassedLessonIdsByUserIdAndModuleIds(userId, moduleIdsOnPage);
        Set<Long> lessonsWithTests = testModelRepository.findLessonIdsWithTestsByModuleIds(moduleIdsOnPage);

        return examsPage.map(exam -> {
            ExamDTO dto = new ExamDTO();
            dto.setTestId(exam.getId());
            dto.setModuleId(exam.getModuleId());
            dto.setModuleSequenceOrder(exam.getModuleSequenceOrder());
            dto.setModuleTitle(exam.getModuleTitle());
            dto.setQuestionsCount(exam.getQuestionsCount());
            dto.setPassThresholdPercentage(exam.getPassThresholdPercentage());

            Set<Long> lessonsInModuleWithTests = lessonsWithTests.stream()
                    .filter(lessonId -> testModelRepository.findByLessonId(lessonId)
                            .map(TestModel::getModule)
                            .map(Module::getId)
                            .map(id -> id.equals(exam.getModuleId()))
                            .orElse(false))
                    .collect(Collectors.toSet());

            boolean isModuleUnblocked = lessonsInModuleWithTests.isEmpty() || passedLessonIds.containsAll(lessonsInModuleWithTests);
            Optional<TestAttempt> lastAttemptOpt = attemptRepository.findFirstByUserIdAndTestIdOrderByCreatedAtDesc(userId, exam.getId());

            if (!isModuleUnblocked) {
                dto.setStatus(ExamStatus.BLOCKED.toString());
                dto.setLastResult(null);
            } else if (lastAttemptOpt.isPresent()) {
                TestAttempt lastAttempt = lastAttemptOpt.get();
                Integer correctAnswers = testAnswerRepository.countByAttemptIdAndEvaluation(lastAttempt.getId(), AnswerEvaluation.CORRECT);
                dto.setLastResult(new LastResultDTO(lastAttempt.getScorePercent(), correctAnswers, exam.getQuestionsCount()));

                if (Boolean.TRUE.equals(lastAttempt.getPassed())) {
                    dto.setStatus(ExamStatus.PASSED.toString());
                } else {
                    dto.setStatus(ExamStatus.FAILED.toString());
                }
            } else {
                dto.setStatus(ExamStatus.AVAILABLE.toString());
                dto.setLastResult(null);
            }
            return dto;
        });
    }

    private void updateTestLesson(TestModel testModel, Long newLessonId) {
        if (testModel.getLesson() != null && testModel.getLesson().getId().equals(newLessonId)) {
            return;
        }

        if (testModelRepository.existsByLessonId(newLessonId)) {
            throw new ResourceConflictException("Another test for lesson ID " + newLessonId + " already exists.");
        }

        Lesson newLesson = findLessonById(newLessonId);
        testModel.setLesson(newLesson);
    }

    private Lesson findLessonById(Long lessonId) {
        return lessonRepository.findById(lessonId)
                .orElseThrow(() -> new ResourceNotFoundException("Lesson not found with id: " + lessonId));
    }

    private TestModel findTestByIdWithQuestions(Long testId) {
        return testModelRepository.findByIdWithQuestions(testId)
                .orElseThrow(() -> new ResourceNotFoundException("Test not found with id: " + testId));
    }
}
