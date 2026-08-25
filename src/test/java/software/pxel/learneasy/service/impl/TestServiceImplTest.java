package software.pxel.learneasy.service.impl;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import software.pxel.learneasy.api.dto.test.ExamDTO;
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
import software.pxel.learneasy.repository.*;
import software.pxel.learneasy.repository.projection.TestModelWithQuestionsCount;
import software.pxel.learneasy.service.QuestionService;

import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("TestService — управление тестами")
class TestServiceImplTest {

    @Mock
    TestModelRepository testModelRepository;
    @Mock
    LessonRepository lessonRepository;
    @Mock
    TestAttemptRepository attemptRepository;
    @Mock
    TestAnswerRepository testAnswerRepository;
    @Mock
    TestModelMapper testModelMapper;
    @Mock
    QuestionService questionService;

    @InjectMocks
    TestServiceImpl service;

    @Nested
    @DisplayName("createTest(request) — создание теста")
    class CreateTest {

        @Test
        @DisplayName("ошибка — для пары урока и модуля уже есть тест (409)")
        void error_conflictLessonTestExists() {
            var req = new TestRequest(TestType.LESSON_TEST, 11L, 22L, "Title", 70, List.of());
            when(testModelRepository.existsByLessonIdAndModuleId(11L, 22L)).thenReturn(true);
            assertThrows(ResourceConflictException.class, () -> service.createTest(req));
            verify(testModelRepository).existsByLessonIdAndModuleId(11L, 22L);
            verifyNoMoreInteractions(testModelRepository);
            verifyNoInteractions(lessonRepository, testModelMapper, questionService);
        }

        @Test
        @DisplayName("ошибка — урок не найден (LESSON_TEST)")
        void error_lessonNotFound_inLessonTest() {
            var req = new TestRequest(TestType.LESSON_TEST, 11L, 22L, "Title", 70, List.of());
            when(lessonRepository.findById(11L)).thenReturn(Optional.empty());
            var ex = assertThrows(ResourceNotFoundException.class, () -> service.createTest(req));
            assertTrue(ex.getMessage().contains("Lesson not found with id: 11"));
            verify(lessonRepository).findById(11L);
            verifyNoInteractions(testModelMapper, questionService);
        }

        @Test
        @DisplayName("успех — создаёт тест для урока (LESSON_TEST), вопросы заменены")
        void success_createLessonTest() {
            var req = new TestRequest(TestType.LESSON_TEST, 11L, 22L, "Title", 70, List.of());
            var lesson = lesson(11L);
            var module = module(22L);
            lesson.setModule(module); // Связываем моки

            when(testModelRepository.existsByLessonIdAndModuleId(11L, 22L)).thenReturn(false);
            when(lessonRepository.findById(11L)).thenReturn(Optional.of(lesson));

            var mapped = new TestModel();
            when(testModelMapper.toLessonTestModel(req, lesson, module)).thenReturn(mapped);

            var persisted = new TestModel();
            persisted.setId(100L);
            when(testModelRepository.save(mapped)).thenReturn(persisted);
            when(testModelRepository.saveAndFlush(persisted)).thenReturn(persisted);

            var resp = new TestResponse(100L, 11L, 22L, null, null, "LESSON_TEST", "Title", 70, List.of());
            when(testModelMapper.toTestResponse(any(TestModel.class))).thenReturn(resp);

            TestResponse out = service.createTest(req);

            assertEquals(100L, out.id());
            verify(questionService).replaceQuestionsForTest(persisted, req.questions());
            verify(testModelRepository).save(mapped);
            verify(testModelRepository).saveAndFlush(persisted);
            verify(testModelMapper).toTestResponse(persisted);
        }

        @Test
        @DisplayName("ошибка — создание экзамена по модулю не поддерживается (UnsupportedOperationException)")
        void error_moduleExamCreation_isUnsupported() {
            var req = new TestRequest(TestType.MODULE_EXAM, null, 33L, "Exam", 60, List.of());
            when(testModelRepository.existsByLessonIdAndModuleId(null, 33L)).thenReturn(false);

            assertThrows(UnsupportedOperationException.class, () -> service.createTest(req));

            verify(testModelRepository).existsByLessonIdAndModuleId(null, 33L);
            verifyNoMoreInteractions(testModelRepository);
            verifyNoInteractions(lessonRepository, testModelMapper, questionService);
        }
    }

    @Nested
    @DisplayName("getTestById(testId) — чтение теста по id")
    class GetTestById {
        // ... тесты без изменений ...
        @Test
        @DisplayName("успех — маппит найденный тест")
        void success_maps() {
            var model = new TestModel();
            model.setId(10L);
            when(testModelRepository.findByIdWithQuestions(10L)).thenReturn(Optional.of(model));
            var resp = new TestResponse(10L, null, 1L, null, null, "MODULE_EXAM", "X", 50, List.of());
            when(testModelMapper.toTestResponse(model)).thenReturn(resp);

            TestResponse out = service.getTestById(10L);

            assertEquals(10L, out.id());
            verify(testModelRepository).findByIdWithQuestions(10L);
            verify(testModelMapper).toTestResponse(model);
        }

        @Test
        @DisplayName("ошибка — тест не найден")
        void error_notFound() {
            when(testModelRepository.findByIdWithQuestions(99L)).thenReturn(Optional.empty());

            var ex = assertThrows(ResourceNotFoundException.class, () -> service.getTestById(99L));
            assertTrue(ex.getMessage().contains("Test not found with id: 99"));

            verify(testModelRepository).findByIdWithQuestions(99L);
            verifyNoInteractions(testModelMapper);
        }
    }

    @Nested
    @DisplayName("getTestByLessonId(lessonId) — чтение теста по уроку")
    class GetTestByLessonId {
        // ... тесты без изменений ...
        @Test
        @DisplayName("успех — маппит найденный тест")
        void success_maps() {
            var model = new TestModel();
            when(testModelRepository.findByLessonIdWithQuestions(7L)).thenReturn(Optional.of(model));
            var resp = new TestResponse(70L, 7L, 5L, null, null, "LESSON_TEST", "T", 55, List.of());
            when(testModelMapper.toTestResponse(model)).thenReturn(resp);

            TestResponse out = service.getTestByLessonId(7L);

            assertEquals(70L, out.id());
            assertEquals(7L, out.lessonId());
            verify(testModelRepository).findByLessonIdWithQuestions(7L);
            verify(testModelMapper).toTestResponse(model);
        }

        @Test
        @DisplayName("ошибка — тест для урока не найден")
        void error_notFound() {
            when(testModelRepository.findByLessonIdWithQuestions(77L)).thenReturn(Optional.empty());

            var ex = assertThrows(ResourceNotFoundException.class, () -> service.getTestByLessonId(77L));
            assertTrue(ex.getMessage().contains("Test not found for lesson id: 77"));

            verify(testModelRepository).findByLessonIdWithQuestions(77L);
            verifyNoInteractions(testModelMapper);
        }
    }

    @Nested
    @DisplayName("updateTest(testId, request) — обновление теста")
    class UpdateTest {
        // ... тесты без изменений ...
        @Test
        @DisplayName("ошибка — тест не найден")
        void error_testNotFound() {
            when(testModelRepository.findByIdWithQuestions(1L)).thenReturn(Optional.empty());

            var req = new TestRequest(TestType.LESSON_TEST, 1L, 2L, "T", 50, List.of());
            var ex = assertThrows(ResourceNotFoundException.class, () -> service.updateTest(1L, req));
            assertTrue(ex.getMessage().contains("Test not found with id: 1"));

            verify(testModelRepository).findByIdWithQuestions(1L);
        }

        @Test
        @DisplayName("успех — урок не меняется, вопросы заменены, патч применён")
        void success_noLessonChange() {
            var existing = new TestModel();
            existing.setId(5L);
            existing.setLesson(lesson(10L));
            when(testModelRepository.findByIdWithQuestions(5L)).thenReturn(Optional.of(existing));

            var req = new TestRequest(TestType.LESSON_TEST, 10L, 2L, "NewTitle", 80, List.of());

            when(testModelRepository.saveAndFlush(existing)).thenReturn(existing);
            var resp = new TestResponse(5L, 10L, 2L, null, null, "LESSON_TEST", "NewTitle", 80, List.of());
            when(testModelMapper.toTestResponse(existing)).thenReturn(resp);

            TestResponse out = service.updateTest(5L, req);

            assertEquals("NewTitle", out.title());
            verify(testModelMapper).updateTestFromRequest(req, existing);
            verify(questionService).replaceQuestionsForTest(existing, req.questions());
            verify(testModelRepository).saveAndFlush(existing);
            verify(testModelMapper).toTestResponse(existing);
            verify(testModelRepository, never()).existsByLessonId(any());
            verify(lessonRepository, never()).findById(any());
        }

        @Test
        @DisplayName("ошибка — смена урока на занятый (409)")
        void error_changeLessonToAlreadyUsed() {
            var existing = new TestModel();
            existing.setId(6L);
            existing.setLesson(lesson(1L));
            when(testModelRepository.findByIdWithQuestions(6L)).thenReturn(Optional.of(existing));

            var req = new TestRequest(TestType.LESSON_TEST, 2L, 3L, "T", 50, List.of());
            when(testModelRepository.existsByLessonId(2L)).thenReturn(true);

            assertThrows(ResourceConflictException.class, () -> service.updateTest(6L, req));

            verify(testModelRepository).findByIdWithQuestions(6L);
            verify(testModelRepository).existsByLessonId(2L);
            verifyNoMoreInteractions(testModelRepository);
            verifyNoInteractions(testModelMapper, questionService, lessonRepository);
        }

        @Test
        @DisplayName("ошибка — новый урок не найден (при смене урока)")
        void error_changeLessonNotFound() {
            var existing = new TestModel();
            existing.setId(7L);
            existing.setLesson(lesson(1L));
            when(testModelRepository.findByIdWithQuestions(7L)).thenReturn(Optional.of(existing));

            var req = new TestRequest(TestType.LESSON_TEST, 3L, 9L, "T", 50, List.of());
            when(testModelRepository.existsByLessonId(3L)).thenReturn(false);
            when(lessonRepository.findById(3L)).thenReturn(Optional.empty());

            var ex = assertThrows(ResourceNotFoundException.class, () -> service.updateTest(7L, req));
            assertTrue(ex.getMessage().contains("Lesson not found with id: 3"));

            verify(testModelRepository).findByIdWithQuestions(7L);
            verify(testModelRepository).existsByLessonId(3L);
            verify(lessonRepository).findById(3L);
            verifyNoInteractions(testModelMapper, questionService);
        }

        @Test
        @DisplayName("успех — смена урока, патч, замена вопросов, сохранение")
        void success_changeLesson() {
            var existing = new TestModel();
            existing.setId(8L);
            existing.setLesson(lesson(1L));
            when(testModelRepository.findByIdWithQuestions(8L)).thenReturn(Optional.of(existing));

            var req = new TestRequest(TestType.LESSON_TEST, 4L, 9L, "T2", 65, List.of());
            when(testModelRepository.existsByLessonId(4L)).thenReturn(false);
            var newLesson = lesson(4L);
            when(lessonRepository.findById(4L)).thenReturn(Optional.of(newLesson));

            when(testModelRepository.saveAndFlush(existing)).thenAnswer(inv -> inv.getArgument(0));
            var resp = new TestResponse(8L, 4L, 9L, null, null, "LESSON_TEST", "T2", 65, List.of());
            when(testModelMapper.toTestResponse(existing)).thenReturn(resp);

            TestResponse out = service.updateTest(8L, req);

            assertEquals(4L, out.lessonId());
            assertSame(newLesson, existing.getLesson());
            verify(testModelMapper).updateTestFromRequest(req, existing);
            verify(questionService).replaceQuestionsForTest(existing, req.questions());
            verify(testModelRepository).saveAndFlush(existing);
            verify(testModelMapper).toTestResponse(existing);
        }
    }

    @Nested
    @DisplayName("deleteTest(testId) — удаление теста")
    class DeleteTest {
        // ... тесты без изменений ...
        @Test
        @DisplayName("ошибка — тест не найден")
        void error_notFound() {
            when(testModelRepository.existsById(99L)).thenReturn(false);
            var ex = assertThrows(ResourceNotFoundException.class, () -> service.deleteTest(99L));
            assertTrue(ex.getMessage().contains("Test not found with id: 99"));
            verify(testModelRepository).existsById(99L);
            verify(testModelRepository, never()).deleteById(anyLong());
        }

        @Test
        @DisplayName("успех — удаляет по id")
        void success_deletes() {
            when(testModelRepository.existsById(5L)).thenReturn(true);
            service.deleteTest(5L);
            verify(testModelRepository).existsById(5L);
            verify(testModelRepository).deleteById(5L);
        }
    }

    @Nested
    @DisplayName("getExamsByCourse(courseId, userId, pageable) — получение экзаменов курса")
    class GetExamsByCourse {
        private final Long COURSE_ID = 1L;
        private final Long USER_ID = 1L;
        private final Pageable PAGEABLE = PageRequest.of(0, 10);

        @Test
        @DisplayName("успех — экзамен заблокирован, если модуль не пройден")
        void success_examIsBlockedIfModuleNotPassed() {
            var examProjection = createExamProjection(10L, 100L, "Intro Module", 15, 75);
            var page = new PageImpl<>(List.of(examProjection));

            when(testModelRepository.findByCourseIdAndTestType(COURSE_ID, TestType.MODULE_EXAM, PAGEABLE)).thenReturn(page);
            when(testModelRepository.findLessonIdsWithTestsByModuleIds(List.of(100L))).thenReturn(Set.of(1001L, 1002L));
            when(attemptRepository.findPassedLessonIdsByUserIdAndModuleIds(USER_ID, List.of(100L))).thenReturn(Set.of(1001L));

            when(testModelRepository.findByLessonId(1001L)).thenReturn(Optional.of(testWithModule(1001L, 100L)));
            when(testModelRepository.findByLessonId(1002L)).thenReturn(Optional.of(testWithModule(1002L, 100L)));


            Page<ExamDTO> result = service.getExamsByCourse(COURSE_ID, USER_ID, PAGEABLE);

            assertEquals(1, result.getTotalElements());
            var dto = result.getContent().getFirst();
            assertEquals(ExamStatus.BLOCKED.toString(), dto.getStatus());
            assertNull(dto.getLastResult());
            verify(testAnswerRepository, never()).countByAttemptIdAndEvaluation(any(), any());
        }

        @Test
        @DisplayName("успех — экзамен доступен, если модуль пройден и попыток нет")
        void success_examIsAvailableIfModulePassedAndNoAttempts() {
            var examProjection = createExamProjection(20L, 200L, "Advanced Module", 20, 80);
            var page = new PageImpl<>(List.of(examProjection));

            when(testModelRepository.findByCourseIdAndTestType(COURSE_ID, TestType.MODULE_EXAM, PAGEABLE)).thenReturn(page);
            when(testModelRepository.findLessonIdsWithTestsByModuleIds(List.of(200L))).thenReturn(Set.of(2001L));
            when(attemptRepository.findPassedLessonIdsByUserIdAndModuleIds(USER_ID, List.of(200L))).thenReturn(Set.of(2001L));
            when(attemptRepository.findFirstByUserIdAndTestIdOrderByCreatedAtDesc(USER_ID, 20L)).thenReturn(Optional.empty());
            when(testModelRepository.findByLessonId(2001L)).thenReturn(Optional.of(testWithModule(2001L, 200L)));

            Page<ExamDTO> result = service.getExamsByCourse(COURSE_ID, USER_ID, PAGEABLE);

            assertEquals(1, result.getTotalElements());
            var dto = result.getContent().getFirst();
            assertEquals(ExamStatus.AVAILABLE.toString(), dto.getStatus());
            assertNull(dto.getLastResult());
        }

        @Test
        @DisplayName("успех — экзамен пройден, если последняя попытка успешна")
        void success_examIsPassedIfLastAttemptIsSuccessful() {
            var examProjection = createExamProjection(30L, 300L, "Expert Module", 10, 90);
            var page = new PageImpl<>(List.of(examProjection));
            var lastAttempt = createTestAttempt(1L, USER_ID, 30L, 95, true);

            when(testModelRepository.findByCourseIdAndTestType(COURSE_ID, TestType.MODULE_EXAM, PAGEABLE)).thenReturn(page);
            when(testModelRepository.findLessonIdsWithTestsByModuleIds(List.of(300L))).thenReturn(Collections.emptySet());
            when(attemptRepository.findPassedLessonIdsByUserIdAndModuleIds(USER_ID, List.of(300L))).thenReturn(Collections.emptySet());
            when(attemptRepository.findFirstByUserIdAndTestIdOrderByCreatedAtDesc(USER_ID, 30L)).thenReturn(Optional.of(lastAttempt));
            when(testAnswerRepository.countByAttemptIdAndEvaluation(1L, AnswerEvaluation.CORRECT)).thenReturn(9);

            Page<ExamDTO> result = service.getExamsByCourse(COURSE_ID, USER_ID, PAGEABLE);

            assertEquals(1, result.getTotalElements());
            var dto = result.getContent().getFirst();
            assertEquals(ExamStatus.PASSED.toString(), dto.getStatus());
            assertNotNull(dto.getLastResult());
            assertEquals(95, dto.getLastResult().getScorePercent());
            assertEquals(9, dto.getLastResult().getCorrect());
            assertEquals(10, dto.getLastResult().getTotal());
        }

        @Test
        @DisplayName("успех — экзамен не сдан, если последняя попытка не успешна")
        void success_examIsFailedIfLastAttemptIsNotSuccessful() {
            var examProjection = createExamProjection(40L, 400L, "Final Exam Module", 50, 60);
            var page = new PageImpl<>(List.of(examProjection));
            var lastAttempt = createTestAttempt(2L, USER_ID, 40L, 55, false);

            when(testModelRepository.findByCourseIdAndTestType(COURSE_ID, TestType.MODULE_EXAM, PAGEABLE)).thenReturn(page);
            when(testModelRepository.findLessonIdsWithTestsByModuleIds(List.of(400L))).thenReturn(Collections.emptySet());
            when(attemptRepository.findPassedLessonIdsByUserIdAndModuleIds(USER_ID, List.of(400L))).thenReturn(Collections.emptySet());
            when(attemptRepository.findFirstByUserIdAndTestIdOrderByCreatedAtDesc(USER_ID, 40L)).thenReturn(Optional.of(lastAttempt));
            when(testAnswerRepository.countByAttemptIdAndEvaluation(2L, AnswerEvaluation.CORRECT)).thenReturn(27);

            Page<ExamDTO> result = service.getExamsByCourse(COURSE_ID, USER_ID, PAGEABLE);

            assertEquals(1, result.getTotalElements());
            var dto = result.getContent().getFirst();
            assertEquals(ExamStatus.FAILED.toString(), dto.getStatus());
            assertNotNull(dto.getLastResult());
            assertEquals(55, dto.getLastResult().getScorePercent());
            assertEquals(27, dto.getLastResult().getCorrect());
            assertEquals(50, dto.getLastResult().getTotal());
        }
    }

    // Хелперы
    private static Lesson lesson(Long id) {
        Lesson l = new Lesson();
        l.setId(id);
        return l;
    }

    private static Module module(Long id) {
        Module m = new Module();
        m.setId(id);
        return m;
    }

    private TestModelWithQuestionsCount createExamProjection(Long testId, Long moduleId, String moduleTitle, Integer questionsCount, Integer passThreshold) {
        TestModelWithQuestionsCount projection = mock(TestModelWithQuestionsCount.class);
        when(projection.getId()).thenReturn(testId);
        when(projection.getModuleId()).thenReturn(moduleId);
        when(projection.getModuleTitle()).thenReturn(moduleTitle);
        when(projection.getQuestionsCount()).thenReturn(questionsCount);
        when(projection.getPassThresholdPercentage()).thenReturn(passThreshold);
        return projection;
    }

    private TestAttempt createTestAttempt(Long id, Long userId, Long testId, Integer scorePercent, boolean passed) {
        TestAttempt attempt = new TestAttempt();
        attempt.setId(id);
        attempt.setUserId(userId);
        TestModel test = new TestModel();
        test.setId(testId);
        attempt.setTest(test);
        attempt.setScorePercent(scorePercent);
        attempt.setPassed(passed);
        return attempt;
    }

    private TestModel testWithModule(Long lessonId, Long moduleId) {
        TestModel test = new TestModel();
        test.setLesson(lesson(lessonId));
        test.setModule(module(moduleId));
        return test;
    }
}
