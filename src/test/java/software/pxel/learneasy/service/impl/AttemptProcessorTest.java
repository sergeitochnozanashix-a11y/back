package software.pxel.learneasy.service.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import software.pxel.learneasy.feign.ai.dto.ResponsesRequest;
import software.pxel.learneasy.feign.ai.dto.ResponsesResponse;
import software.pxel.learneasy.feign.ai.service.AIGatewayService;
import software.pxel.learneasy.model.Course;
import software.pxel.learneasy.model.Module;
import software.pxel.learneasy.model.Question;
import software.pxel.learneasy.model.TestAnswer;
import software.pxel.learneasy.model.TestAttempt;
import software.pxel.learneasy.model.TestModel;
import software.pxel.learneasy.model.enums.AnswerEvaluation;
import software.pxel.learneasy.model.enums.AnswerInputType;
import software.pxel.learneasy.model.enums.AttemptStatus;
import software.pxel.learneasy.repository.TestAnswerRepository;
import software.pxel.learneasy.repository.TestAttemptRepository;
import software.pxel.learneasy.service.async.AttemptProcessor;
import software.pxel.learneasy.util.PromptManager;

import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
@DisplayName("AttemptProcessor — обработка одной попытки теста")
class AttemptProcessorTest {

    @Mock
    private TestAttemptRepository attemptRepository;
    @Mock
    private TestAnswerRepository answerRepository;
    @Mock
    private AIGatewayService aiGatewayService;
    @Mock
    private PromptManager promptManager;
    @Spy
    private ObjectMapper objectMapper = new ObjectMapper();

    @InjectMocks
    private AttemptProcessor attemptProcessor;

    private TestAttempt attempt;
    private TestModel test;
    private Question q1;
    private Question q2;
    private Question q3;

    @BeforeEach
    void setUp() {
        attemptProcessor.setSelf(attemptProcessor);

        when(promptManager.getAssessmentInstructions()).thenReturn("Dummy AI instructions for test");

        Course course = createCourse(99L);
        Module module = createModule(999L, course);
        test = new TestModel();
        test.setId(1L);
        test.setPassThresholdPercentage(70);
        test.setModule(module);

        attempt = new TestAttempt();
        attempt.setId(10L);
        attempt.setUserId(1L);
        attempt.setTest(test);
        attempt.setStatus(AttemptStatus.SUBMITTED);

        q1 = createQuestion(101L, "Question 1?", 10);
        q2 = createQuestion(102L, "Question 2?", 10);
        q3 = createQuestion(103L, "Question 3?", 5);
    }

    @Nested
    @DisplayName("evaluateAttempt(attemptId)")
    class EvaluateAttempt {

        @Test
        @DisplayName("не делает ничего, если попытка не найдена")
        void doesNothing_whenAttemptNotFound() {
            when(attemptRepository.findByIdWithTest(anyLong())).thenReturn(Optional.empty());
            attemptProcessor.evaluateAttempt(99L);
            verify(attemptRepository).findByIdWithTest(99L);
            verify(attemptRepository, never()).save(any());
            verifyNoMoreInteractions(answerRepository, aiGatewayService);
        }

        @Test
        @DisplayName("завершает оценку с 0%, если у попытки нет ответов")
        void finalizesWithZeroScore_whenNoAnswers() {
            when(attemptRepository.findByIdWithTest(10L)).thenReturn(Optional.of(attempt));
            when(answerRepository.findByAttemptIdWithQuestion(10L)).thenReturn(Collections.emptyList());

            attemptProcessor.evaluateAttempt(10L);

            assertEquals(AttemptStatus.EVALUATED, attempt.getStatus());
            assertEquals(0, attempt.getScorePercent());
            assertFalse(attempt.getPassed());
            assertNotNull(attempt.getEvaluatedAt());
            verify(attemptRepository, times(2)).save(attempt); // 1 for EVALUATING, 1 for final
            verify(aiGatewayService, never()).responses(any());
        }

        @Test
        @DisplayName("обрабатывает пустые ответы как INCORRECT")
        void processesBlankAnswersAsIncorrect() {
            TestAnswer blankAnswer = createTestAnswer(1L, attempt, q1, AnswerInputType.TEXT, "  ");
            TestAnswer nullAnswer = createTestAnswer(2L, attempt, q2, null, null);

            when(attemptRepository.findByIdWithTest(10L)).thenReturn(Optional.of(attempt));
            when(answerRepository.findByAttemptIdWithQuestion(10L)).thenReturn(List.of(blankAnswer, nullAnswer));

            attemptProcessor.evaluateAttempt(10L);

            assertEquals(AnswerEvaluation.INCORRECT, blankAnswer.getEvaluation());
            assertEquals("Ответ не был дан.", blankAnswer.getEvaluationNotes());
            assertEquals(AnswerEvaluation.INCORRECT, nullAnswer.getEvaluation());
            assertEquals("Ответ не был дан.", nullAnswer.getEvaluationNotes());
            verify(attemptRepository, times(2)).save(attempt);
            verify(aiGatewayService, never()).responses(any());
        }

        @Test
        @DisplayName("успешно оценивает ответы, вызывает AI и рассчитывает итоговый балл")
        void successfullyEvaluatesAnswersAndCalculatesScore() {
            TestAnswer correct = createTestAnswer(1L, attempt, q1, AnswerInputType.TEXT, "Correct answer");
            TestAnswer partial = createTestAnswer(2L, attempt, q2, AnswerInputType.TEXT, "Partial answer");
            TestAnswer incorrect = createTestAnswer(3L, attempt, q3, AnswerInputType.TEXT, "Incorrect answer");

            when(attemptRepository.findByIdWithTest(10L)).thenReturn(Optional.of(attempt));
            when(answerRepository.findByAttemptIdWithQuestion(10L)).thenReturn(List.of(correct, partial, incorrect));

            when(aiGatewayService.responses(any(ResponsesRequest.class)))
                    .thenAnswer(invocation -> {
                        ResponsesRequest req = invocation.getArgument(0);
                        if (req.input() instanceof String inputString) {
                            if (inputString.contains("Correct answer")) {
                                return mockAIResponse(AnswerEvaluation.CORRECT, "Отлично!");
                            }
                            if (inputString.contains("Partial answer")) {
                                return mockAIResponse(AnswerEvaluation.PARTIAL, "Почти верно.");
                            }
                            if (inputString.contains("Incorrect answer")) {
                                return mockAIResponse(AnswerEvaluation.INCORRECT, "Неправильно.");
                            }
                        }
                        return null;
                    });

            attemptProcessor.evaluateAttempt(10L);

            assertEquals(AnswerEvaluation.CORRECT, correct.getEvaluation());
            assertEquals("Отлично!", correct.getEvaluationNotes());
            assertEquals(AnswerEvaluation.PARTIAL, partial.getEvaluation());
            assertEquals("Почти верно.", partial.getEvaluationNotes());
            assertEquals(AnswerEvaluation.INCORRECT, incorrect.getEvaluation());
            assertEquals("Неправильно.", incorrect.getEvaluationNotes());
            verify(aiGatewayService, times(3)).responses(any(ResponsesRequest.class));
            verify(answerRepository).saveAll(anyList());
            verify(attemptRepository, times(2)).save(attempt);

            assertEquals(AttemptStatus.EVALUATED, attempt.getStatus());
            assertEquals(60, attempt.getScorePercent());
            assertFalse(attempt.getPassed());
        }

        @Test
        @DisplayName("переводит попытку в FAILED, если AI вернул некорректный JSON")
        void marksAttemptAsFailed_whenAIResponseIsMalformed() {
            TestAnswer answer = createTestAnswer(1L, attempt, q1, AnswerInputType.TEXT, "Some answer");
            when(attemptRepository.findByIdWithTest(10L)).thenReturn(Optional.of(attempt));
            when(answerRepository.findByAttemptIdWithQuestion(10L)).thenReturn(List.of(answer));

            ResponsesResponse malformedAiResponse = mockAIResponseWithText("this is not a json");
            when(aiGatewayService.responses(any())).thenReturn(malformedAiResponse);

            attemptProcessor.evaluateAttempt(10L);

            assertEquals(AttemptStatus.FAILED, attempt.getStatus());
            assertNotNull(attempt.getFailedReason());
            assertTrue(attempt.getFailedReason().contains("AI evaluation service failure"));
            verify(attemptRepository, times(2)).save(attempt);
        }

        @Test
        @DisplayName("переводит попытку в FAILED, если сервис AI бросает исключение")
        void marksAttemptAsFailed_whenAIServiceThrowsException() {
            TestAnswer answer = createTestAnswer(1L, attempt, q1, AnswerInputType.TEXT, "Some answer");
            when(attemptRepository.findByIdWithTest(10L)).thenReturn(Optional.of(attempt));
            when(answerRepository.findByAttemptIdWithQuestion(10L)).thenReturn(List.of(answer));

            when(aiGatewayService.responses(any())).thenThrow(new RuntimeException("AI service is down"));

            attemptProcessor.evaluateAttempt(10L);

            assertEquals(AttemptStatus.FAILED, attempt.getStatus());
            assertNotNull(attempt.getFailedReason());
            assertTrue(attempt.getFailedReason().contains("AI service is down"));
            verify(attemptRepository, times(2)).save(attempt);
        }
    }

    private Course createCourse(Long id) {
        Course c = new Course();
        c.setId(id);
        return c;
    }

    private Module createModule(Long id, Course course) {
        Module m = new Module();
        m.setId(id);
        m.setCourse(course);
        return m;
    }

    private Question createQuestion(Long id, String text, int maxScore) {
        Question q = Question.builder().text(text).maxScore(maxScore).build();
        q.setId(id);
        return q;
    }

    private TestAnswer createTestAnswer(Long id, TestAttempt attempt, Question question, AnswerInputType type, String text) {
        TestAnswer answer = TestAnswer.builder()
                .attempt(attempt)
                .question(question)
                .inputType(type)
                .textAnswer(text)
                .build();
        answer.setId(id);
        return answer;
    }

    private ResponsesResponse mockAIResponse(AnswerEvaluation evaluation, String feedback) {
        String jsonText = String.format("{\"evaluation\":\"%s\", \"feedback\":\"%s\"}", evaluation.name(), feedback);
        return mockAIResponseWithText(jsonText);
    }

    private ResponsesResponse mockAIResponseWithText(String text) {
        ResponsesResponse.Content content = new ResponsesResponse.Content("output_text", text);
        ResponsesResponse.Message message = new ResponsesResponse.Message("m1", "message", "assistant", List.of(content));
        return new ResponsesResponse("id", "response", 1L, "model", "completed", List.of(message), null, null);
    }
}
