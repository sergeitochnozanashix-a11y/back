package software.pxel.learneasy.service.impl;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import software.pxel.learneasy.api.dto.attempt.AttemptAnswerCreate;
import software.pxel.learneasy.api.dto.attempt.CreateAttemptRequest;
import software.pxel.learneasy.api.dto.attempt.TestAttemptResponse;
import software.pxel.learneasy.exception.ResourceConflictException;
import software.pxel.learneasy.exception.ResourceNotFoundException;
import software.pxel.learneasy.mapper.TestAttemptMapper;
import software.pxel.learneasy.model.Lesson;
import software.pxel.learneasy.model.Module;
import software.pxel.learneasy.model.Question;
import software.pxel.learneasy.model.TestAnswer;
import software.pxel.learneasy.model.TestAttempt;
import software.pxel.learneasy.model.TestModel;
import software.pxel.learneasy.model.enums.AnswerInputType;
import software.pxel.learneasy.model.enums.AttemptStatus;
import software.pxel.learneasy.repository.TestAnswerRepository;
import software.pxel.learneasy.repository.TestAttemptRepository;
import software.pxel.learneasy.repository.TestModelRepository;
import software.pxel.learneasy.service.async.AIEvaluationQueue;
import software.pxel.learneasy.service.util.AfterCommitExecutor;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.stream.StreamSupport;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("TestAttemptService — попытки прохождения теста")
class TestAttemptServiceImplTest {

    @Mock
    TestAttemptRepository attemptRepository;
    @Mock
    TestAnswerRepository answerRepository;
    @Mock
    TestModelRepository testRepository;
    @Mock
    TestAttemptMapper mapper;
    @Mock
    AIEvaluationQueue evaluationQueue;
    @Mock
    AfterCommitExecutor afterCommitExecutor;

    private TestAttemptServiceImpl service;

    @BeforeEach
    void setUp() {
        service = new TestAttemptServiceImpl(
                attemptRepository, answerRepository,
                testRepository, mapper, evaluationQueue, afterCommitExecutor
        );
    }

    // ------------------------------------------------------------
    @Nested
    @DisplayName("createAttempt(userId, request)")
    class CreateAttempt {

        @Test
        @DisplayName("ошибка — тест не найден")
        void error_testNotFound() {
            var req = new CreateAttemptRequest(42L, List.of());
            when(testRepository.findByIdWithQuestions(42L)).thenReturn(Optional.empty());

            var ex = assertThrows(ResourceNotFoundException.class, () -> service.createAttempt(7L, req));
            assertTrue(ex.getMessage().contains("Test not found: 42"));

            verify(testRepository).findByIdWithQuestions(42L);
            verifyNoInteractions(attemptRepository, answerRepository, mapper, evaluationQueue, afterCommitExecutor);
        }

        @Test
        @DisplayName("ошибка — у теста отсутствует module")
        void error_missingModule() {
            TestModel test = new TestModel();
            test.setId(1L);
            test.setQuestions(List.of());
            when(testRepository.findByIdWithQuestions(1L)).thenReturn(Optional.of(test));

            var req = new CreateAttemptRequest(1L, List.of());

            test.setLesson(null);
            test.setModule(null);
            var ex1 = assertThrows(ResourceConflictException.class, () -> service.createAttempt(10L, req));
            assertTrue(ex1.getMessage().contains("Test has no module"));

            test.setLesson(lesson(100L));
            test.setModule(null);

            var ex2 = assertThrows(ResourceConflictException.class, () -> service.createAttempt(10L, req));
            assertTrue(ex2.getMessage().contains("Test has no module"));
        }

        @Test
        @DisplayName("ошибка — переданы ответы на неизвестные вопросы")
        void error_unknownQuestionIds() {
            var q1 = question(1L);
            TestModel test = testWith(lesson(10L), module(20L), List.of(q1));
            when(testRepository.findByIdWithQuestions(5L)).thenReturn(Optional.of(test));

            var badAnswer = new AttemptAnswerCreate(999L, AnswerInputType.TEXT, "hi", null, null);
            var req = new CreateAttemptRequest(5L, List.of(badAnswer));

            var ex = assertThrows(ResourceConflictException.class, () -> service.createAttempt(1L, req));

            String errorMessage = ex.getMessage();
            assertTrue(errorMessage.contains("Request contains questionId(s) not belonging to this test:"));
            assertTrue(errorMessage.contains("999"));

            verify(testRepository).findByIdWithQuestions(5L);
            verifyNoInteractions(attemptRepository, answerRepository, mapper, evaluationQueue, afterCommitExecutor);
        }

        @Test
        @DisplayName("ошибка — дублирующиеся questionId")
        void error_duplicateQuestionIds() {
            var q1 = question(1L);
            var q2 = question(2L);
            TestModel test = testWith(lesson(10L), module(20L), List.of(q1, q2));
            when(testRepository.findByIdWithQuestions(6L)).thenReturn(Optional.of(test));

            var a = new AttemptAnswerCreate(1L, AnswerInputType.TEXT, "a", null, null);
            var req = new CreateAttemptRequest(6L, List.of(a, a));

            var ex = assertThrows(ResourceConflictException.class, () -> service.createAttempt(2L, req));
            assertTrue(ex.getMessage().contains("Duplicate questionId submitted: 1"));
        }

        @Test
        @DisplayName("ошибка — слишком большой payload текстов/транскриптов")
        void error_payloadTooLarge() {
            var q1 = question(1L);
            TestModel test = testWith(lesson(10L), module(20L), List.of(q1));
            when(testRepository.findByIdWithQuestions(7L)).thenReturn(Optional.of(test));

            String big = "x".repeat(1001);
            var a = new AttemptAnswerCreate(999L, AnswerInputType.TEXT, big, null, null);
            var req = new CreateAttemptRequest(7L, List.of(a));

            var ex = assertThrows(ResourceConflictException.class, () -> service.createAttempt(3L, req));
            assertTrue(ex.getMessage().contains("Request contains questionId(s) not belonging to this test: "));
        }

        @Test
        @DisplayName("ошибка — TEXT: нет текста или присутствуют voice-поля")
        void error_textIncoherent() {
            var q1 = question(1L);
            TestModel test = testWith(lesson(10L), module(20L), List.of(q1));
            when(testRepository.findByIdWithQuestions(8L)).thenReturn(Optional.of(test));

            var bad1 = new AttemptAnswerCreate(1L, AnswerInputType.TEXT, "   ", null, null);
            assertThrows(ResourceConflictException.class, () -> service.createAttempt(1L, new CreateAttemptRequest(8L, List.of(bad1))));

            var bad2 = new AttemptAnswerCreate(1L, AnswerInputType.TEXT, "x", "http://voice", null);
            assertThrows(ResourceConflictException.class, () -> service.createAttempt(1L, new CreateAttemptRequest(8L, List.of(bad2))));
        }

        @Test
        @DisplayName("ошибка — VOICE: нет url или присутствует text")
        void error_voiceIncoherent() {
            var q1 = question(1L);
            TestModel test = testWith(lesson(10L), module(20L), List.of(q1));
            when(testRepository.findByIdWithQuestions(9L)).thenReturn(Optional.of(test));

            var bad1 = new AttemptAnswerCreate(1L, AnswerInputType.VOICE, null, "   ", "text");
            assertThrows(ResourceConflictException.class, () -> service.createAttempt(1L, new CreateAttemptRequest(9L, List.of(bad1))));

            var bad2 = new AttemptAnswerCreate(1L, AnswerInputType.VOICE, "text", "http://voice", "tr");
            assertThrows(ResourceConflictException.class, () -> service.createAttempt(1L, new CreateAttemptRequest(9L, List.of(bad2))));
        }

        @Test
        @DisplayName("успех — пустые ответы: создаёт пустые TestAnswer и ставит в очередь")
        void success_emptyAnswers_createsSkeletonAndQueues() {
            doAnswer(invocation -> {
                Runnable task = invocation.getArgument(0);
                task.run();
                return null;
            }).when(afterCommitExecutor).execute(any(Runnable.class));

            var qs = List.of(question(1L), question(2L), question(3L));
            TestModel test = testWith(lesson(10L), module(20L), qs);
            when(testRepository.findByIdWithQuestions(10L)).thenReturn(Optional.of(test));

            var req = new CreateAttemptRequest(10L, null);

            TestAttempt persisted = new TestAttempt();
            persisted.setId(99L);
            persisted.setStatus(AttemptStatus.SUBMITTED);
            when(attemptRepository.save(any(TestAttempt.class))).thenReturn(persisted);

            when(mapper.toResponse(eq(persisted), anyList())).thenReturn(
                    new TestAttemptResponse(99L, 1L, 10L, 10L, 20L, AttemptStatus.SUBMITTED, List.of(), null, null, null, null, null, null)
            );

            TestAttemptResponse resp = service.createAttempt(1L, req);
            assertEquals(AttemptStatus.SUBMITTED, resp.status());

            verify(afterCommitExecutor).execute(any(Runnable.class));
            verify(evaluationQueue).submitForEvaluation(99L);

            ArgumentCaptor<Iterable<TestAnswer>> cap = ArgumentCaptor.forClass(Iterable.class);
            verify(answerRepository).saveAll(cap.capture());
            var savedList = toList(cap.getValue());
            assertEquals(3, savedList.size());
        }

        @Test
        @DisplayName("успех — частичные ответы: корректное заполнение и постановка в очередь")
        void success_partialAnswers_textAndVoiceAndQueues() {
            doAnswer(invocation -> {
                Runnable task = invocation.getArgument(0);
                task.run();
                return null;
            }).when(afterCommitExecutor).execute(any(Runnable.class));

            var q1 = question(1L);
            var q2 = question(2L);
            var q3 = question(3L);
            TestModel test = testWith(lesson(10L), module(20L), List.of(q1, q2, q3));
            when(testRepository.findByIdWithQuestions(11L)).thenReturn(Optional.of(test));

            var a1 = new AttemptAnswerCreate(1L, AnswerInputType.TEXT, "hello", null, null);
            var a2 = new AttemptAnswerCreate(2L, AnswerInputType.VOICE, null, "http://voice", "hi");
            var req = new CreateAttemptRequest(11L, List.of(a1, a2));

            TestAttempt persisted = new TestAttempt();
            persisted.setId(100L);
            persisted.setStatus(AttemptStatus.SUBMITTED);
            when(attemptRepository.save(any(TestAttempt.class))).thenReturn(persisted);

            when(mapper.toResponse(eq(persisted), anyList())).thenReturn(
                    new TestAttemptResponse(100L, 1L, 11L, 10L, 20L, AttemptStatus.SUBMITTED, List.of(), null, null, null, null, null, null)
            );

            service.createAttempt(1L, req);

            verify(afterCommitExecutor).execute(any(Runnable.class));
            verify(evaluationQueue).submitForEvaluation(100L);
        }
    }

    // ------------------------------------------------------------
    @Nested
    @DisplayName("getLastAttempt(userId, testId)")
    class GetLastAttempt {

        @Test
        @DisplayName("успех — маппит последнюю попытку")
        void success_maps() {
            TestAttempt a = new TestAttempt();
            a.setId(1L);
            when(attemptRepository.findFirstByUserIdAndTest_IdOrderByCreatedAtDesc(1L, 2L))
                    .thenReturn(Optional.of(a));
            when(answerRepository.findByAttemptIdWithQuestion(1L)).thenReturn(List.of());
            when(mapper.toResponse(eq(a), anyList())).thenReturn(
                    new TestAttemptResponse(1L, 1L, 2L, null, null, AttemptStatus.SUBMITTED, List.of(), null, null, null, null, null, null)
            );

            service.getLastAttempt(1L, 2L);
            verify(answerRepository).findByAttemptIdWithQuestion(1L);
            verify(mapper).toResponse(eq(a), anyList());
        }
    }

    // ------------------------------------------------------------
    @Nested
    @DisplayName("getAttempts(userId, testId, pageable)")
    class GetAttempts {

        @Test
        @DisplayName("успех — постраничный маппинг с подгрузкой ответов")
        void success_pagedMapping() {
            var pageable = PageRequest.of(0, 2);
            TestAttempt a1 = new TestAttempt();
            a1.setId(1L);
            TestAttempt a2 = new TestAttempt();
            a2.setId(2L);
            List<TestAttempt> attempts = List.of(a1, a2);
            when(attemptRepository.findByUserIdAndTest_IdOrderByCreatedAtDesc(5L, 6L, pageable))
                    .thenReturn(new PageImpl<>(attempts, pageable, 2));

            when(answerRepository.findByAttemptInWithQuestion(attempts)).thenReturn(List.of());

            when(mapper.toResponse(any(), anyList())).thenAnswer(invocation -> {
                TestAttempt arg = invocation.getArgument(0);
                return new TestAttemptResponse(arg.getId(), 5L, 6L, null, null, AttemptStatus.SUBMITTED, List.of(), null, null, null, null, null, null);
            });

            service.getAttempts(5L, 6L, pageable);
            verify(answerRepository).findByAttemptInWithQuestion(attempts);
            verify(mapper, times(2)).toResponse(any(), anyList());
        }
    }

    // ------------------------------------------------------------
    // Хелперы
    private static <T> List<T> toList(Iterable<T> it) {
        return (it instanceof Collection<T> c)
                ? new ArrayList<>(c)
                : StreamSupport.stream(it.spliterator(), false).toList();
    }

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

    private static Question question(Long id) {
        Question q = new Question();
        q.setId(id);
        return q;
    }

    private static TestModel testWith(Lesson lesson, Module module, List<Question> questions) {
        TestModel t = new TestModel();
        t.setId(100L);
        t.setLesson(lesson);
        t.setModule(module);
        t.setQuestions(new ArrayList<>(questions));
        return t;
    }
}
