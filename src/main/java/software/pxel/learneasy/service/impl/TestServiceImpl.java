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
import software.pxel.learneasy.exception.BadRequestException;
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
import software.pxel.learneasy.repository.ModuleRepository;
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
    private final ModuleRepository moduleRepository;
    private final TestAttemptRepository attemptRepository;
    private final TestModelMapper testModelMapper;
    private final QuestionService questionService;
    private final TestAnswerRepository testAnswerRepository;

    @Override
    @Transactional
    public TestResponse createTest(TestRequest testRequest) {
        // Ветвимся по testType, а не по наличию lessonId. Прежняя развилка
        // "если lessonId задан - урок, иначе экзамен" игнорировала объявленный
        // тип и упиралась в UnsupportedOperationException: экзамен по модулю
        // отдавал 500, а LESSON_TEST без урока - тоже 500 вместо внятного 400.
        TestModel testModel = switch (testRequest.testType()) {
            case LESSON_TEST -> buildLessonTest(testRequest);
            case MODULE_EXAM -> buildModuleExam(testRequest);
        };

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

    private TestModel buildLessonTest(TestRequest testRequest) {
        if (testRequest.lessonId() == null) {
            throw new BadRequestException("lessonId обязателен для теста типа LESSON_TEST.");
        }

        if (testModelRepository.existsByLessonId(testRequest.lessonId())) {
            throw new ResourceConflictException(
                    "Test for lesson ID " + testRequest.lessonId() + " already exists.");
        }

        Lesson lesson = findLessonById(testRequest.lessonId());
        return testModelMapper.toTestModel(testRequest, lesson, lesson.getModule());
    }

    private TestModel buildModuleExam(TestRequest testRequest) {
        // lessonId для экзамена не нужен и намеренно игнорируется: экзамен
        // привязан к модулю целиком.
        Module module = moduleRepository.findById(testRequest.moduleId())
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Module not found with id: " + testRequest.moduleId()));

        if (testModelRepository.existsByModuleIdAndTestType(module.getId(), TestType.MODULE_EXAM)) {
            throw new ResourceConflictException(
                    "Exam for module ID " + module.getId() + " already exists.");
        }

        return testModelMapper.toTestModel(testRequest, null, module);
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
