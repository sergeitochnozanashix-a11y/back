package software.pxel.learneasy.service.async;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Caching;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import software.pxel.learneasy.exception.AIAssessmentException;
import software.pxel.learneasy.feign.ai.dto.ResponsesRequest;
import software.pxel.learneasy.feign.ai.dto.ResponsesResponse;
import software.pxel.learneasy.feign.ai.service.AIGatewayService;
import software.pxel.learneasy.model.TestAnswer;
import software.pxel.learneasy.model.TestAttempt;
import software.pxel.learneasy.model.enums.AnswerEvaluation;
import software.pxel.learneasy.model.enums.AttemptStatus;
import software.pxel.learneasy.repository.TestAnswerRepository;
import software.pxel.learneasy.repository.TestAttemptRepository;
import software.pxel.learneasy.service.dto.AIEvaluationResult;
import software.pxel.learneasy.util.PromptManager;

import java.time.Instant;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicInteger;

@Service
@RequiredArgsConstructor
@Slf4j
public class AttemptProcessor {

    private final TestAttemptRepository attemptRepository;
    private final TestAnswerRepository answerRepository;
    private final AIGatewayService aiGatewayService;
    private final ObjectMapper objectMapper;
    private final PromptManager promptManager;
    private AttemptProcessor self;

    @Value("${ai.model.name:gpt-5-mini}")
    private String aiModelName;

    //TODO Заменить Self-injection перенос логики в отдельный сервис
    @Autowired
    public void setSelf(@Lazy AttemptProcessor self) {
        this.self = self;
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    @Caching(evict = {
            @CacheEvict(value = "courseProgress", key = "#userId + '::' + #courseId"),
            @CacheEvict(value = "modulesProgress", key = "#userId + '::' + #courseId"),
            @CacheEvict(value = "mainPageInfo", key = "#userId")
    })
    public void finalizeAndEvict(TestAttempt attempt, int scorePercent, int passThreshold, Long userId, Long courseId) {
        attempt.setScorePercent(scorePercent);
        attempt.setPassed(scorePercent >= passThreshold);
        attempt.setStatus(AttemptStatus.EVALUATED);
        attempt.setEvaluatedAt(Instant.now());
        log.info("Successfully evaluated and evicting cache for TestAttempt with id=[{}]. Final score: {}%", attempt.getId(), scorePercent);
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void evaluateAttempt(Long attemptId) {
        TestAttempt attempt = attemptRepository.findByIdWithTest(attemptId)
                .orElse(null);

        if (attempt == null) {
            log.error("Could not find TestAttempt with id=[{}] for evaluation.", attemptId);
            return;
        }

        Long userId = attempt.getUserId();
        Long courseId = attempt.getTest().getModule().getCourse().getId();

        try {
            attempt.setStatus(AttemptStatus.EVALUATING);
            attemptRepository.save(attempt); // Принудительно сохраняем промежуточный статус

            List<TestAnswer> answers = answerRepository.findByAttemptIdWithQuestion(attemptId);
            if (answers.isEmpty()) {
                self.finalizeAndEvict(attempt, 0, attempt.getTest().getPassThresholdPercentage(), userId, courseId);
                return;
            }

            for (TestAnswer answer : answers) {
                processAnswer(answer);
            }

            answerRepository.saveAll(answers);
            calculateFinalScoreAndFinalize(attempt, answers, userId, courseId);

        } catch (Exception e) {
            log.error("Failed to evaluate TestAttempt with id=[{}]. Marking as FAILED.", attemptId, e);
            attempt.setStatus(AttemptStatus.FAILED);
            attempt.setFailedReason("AI evaluation service failure: " + e.getMessage());
        } finally {
            attemptRepository.save(attempt);
        }
    }

    private void processAnswer(TestAnswer answer) {
        if (answer.getInputType() == null || isBlank(getAnswerText(answer))) {
            answer.setEvaluation(AnswerEvaluation.INCORRECT);
            answer.setEvaluationNotes("Ответ не был дан.");
            return;
        }

        String formattedInput = String.format(
                "Текст вопроса для проверки: \"%s\"%n%nОтвет студента: \"%s\"",
                answer.getQuestion().getText(),
                getAnswerText(answer)
        );

        var request = new ResponsesRequest(aiModelName, promptManager.getAssessmentInstructions(), formattedInput);
        ResponsesResponse response = aiGatewayService.responses(request);
        AIEvaluationResult result = parseAIResponse(response);

        answer.setEvaluation(result.evaluation() != null ? result.evaluation() : AnswerEvaluation.UNASSESSED);
        answer.setEvaluationNotes(result.feedback());
    }

    private void calculateFinalScoreAndFinalize(TestAttempt attempt, List<TestAnswer> answers, Long userId, Long courseId) {
        if (answers.isEmpty()) {
            self.finalizeAndEvict(attempt, 0, attempt.getTest().getPassThresholdPercentage(), userId, courseId);
            return;
        }

        AtomicInteger totalMaxScore = new AtomicInteger(0);
        double achievedScore = 0.0;

        answers.forEach(answer -> totalMaxScore.addAndGet(answer.getQuestion().getMaxScore()));

        for (TestAnswer answer : answers) {
            switch (answer.getEvaluation()) {
                case CORRECT -> achievedScore += answer.getQuestion().getMaxScore();
                case PARTIAL -> achievedScore += (double) answer.getQuestion().getMaxScore() / 2;
                default -> {
                }
            }
        }

        int finalPercentage = totalMaxScore.get() > 0
                ? (int) Math.round((achievedScore / totalMaxScore.get()) * 100)
                : 0;

        self.finalizeAndEvict(attempt, finalPercentage, attempt.getTest().getPassThresholdPercentage(), userId, courseId);
    }

    private AIEvaluationResult parseAIResponse(ResponsesResponse response) {
        if (response == null) {
            throw new AIAssessmentException("AI response object is null.");
        }

        String responseText = extractTextFromResponse(response);

        if (responseText == null || responseText.isBlank()) {
            throw new AIAssessmentException("AI response is empty or malformed (could not extract text).");
        }

        try {
            return objectMapper.readValue(responseText, AIEvaluationResult.class);
        } catch (JsonProcessingException e) {
            log.error("Failed to parse AI JSON response from extracted text: {}", responseText, e);
            throw new AIAssessmentException("AI returned invalid JSON structure inside its text block.", e);
        }
    }

    private String extractTextFromResponse(ResponsesResponse response) {
        if (response.outputText() != null && !response.outputText().isBlank()) {
            return response.outputText();
        }

        if (response.output() != null && !response.output().isEmpty()) {
            return response.output().stream()
                    .filter(message -> "message".equals(message.type()) && message.content() != null)
                    .flatMap(message -> message.content().stream())
                    .filter(content -> "output_text".equals(content.type()) && content.text() != null)
                    .map(ResponsesResponse.Content::text)
                    .findFirst()
                    .orElse(null);
        }
        return null;
    }

    private String getAnswerText(TestAnswer answer) {
        return Objects.requireNonNullElse(answer.getTextAnswer(), answer.getVoiceTranscript());
    }

    private boolean isBlank(String s) {
        return s == null || s.trim().isEmpty();
    }
}
