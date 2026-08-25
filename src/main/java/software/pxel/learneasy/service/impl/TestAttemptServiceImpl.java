package software.pxel.learneasy.service.impl;

import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import software.pxel.learneasy.api.dto.attempt.AttemptAnswerCreate;
import software.pxel.learneasy.api.dto.attempt.CreateAttemptRequest;
import software.pxel.learneasy.api.dto.attempt.TestAttemptResponse;
import software.pxel.learneasy.exception.ResourceConflictException;
import software.pxel.learneasy.exception.ResourceNotFoundException;
import software.pxel.learneasy.mapper.TestAttemptMapper;
import software.pxel.learneasy.model.*;
import software.pxel.learneasy.model.Module;
import software.pxel.learneasy.model.enums.AnswerEvaluation;
import software.pxel.learneasy.model.enums.AnswerInputType;
import software.pxel.learneasy.model.enums.AttemptStatus;
import software.pxel.learneasy.repository.TestAnswerRepository;
import software.pxel.learneasy.repository.TestAttemptRepository;
import software.pxel.learneasy.repository.TestModelRepository;
import software.pxel.learneasy.service.util.AfterCommitExecutor;
import software.pxel.learneasy.service.async.AIEvaluationQueue;
import software.pxel.learneasy.service.TestAttemptService;

import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class TestAttemptServiceImpl implements TestAttemptService {

    private final TestAttemptRepository attemptRepository;
    private final TestAnswerRepository answerRepository;
    private final TestModelRepository testRepository;
    private final TestAttemptMapper mapper;
    private final AIEvaluationQueue evaluationQueue;
    private final AfterCommitExecutor afterCommitExecutor;

    @Override
    @Transactional
    public TestAttemptResponse createAttempt(Long userId, CreateAttemptRequest request) {
        TestModel test = testRepository.findByIdWithQuestions(request.testId())
                .orElseThrow(() -> new ResourceNotFoundException("Test not found: " + request.testId()));

        Module module = resolveModule(test);

        List<AttemptAnswerCreate> providedAnswers = Optional.ofNullable(request.answers()).orElseGet(List::of);

        validateAnswersSubset(providedAnswers, test);

        TestAttempt attempt = TestAttempt.builder()
                .userId(userId)
                .test(test)
                .lesson(test.getLesson())
                .module(module)
                .status(AttemptStatus.SUBMITTED)
                .build();
        attempt = attemptRepository.save(attempt);

        Map<Long, AttemptAnswerCreate> providedByQid = providedAnswers.stream()
                .collect(Collectors.toMap(AttemptAnswerCreate::questionId, Function.identity()));

        List<TestAnswer> toSave = new ArrayList<>(test.getQuestions().size());

        for (Question q : test.getQuestions()) {
            AttemptAnswerCreate provided = providedByQid.get(q.getId());
            if (provided != null) {
                validateInputCoherence(provided);
                toSave.add(
                        TestAnswer.builder()
                                .attempt(attempt)
                                .question(q)
                                .inputType(provided.inputType())
                                .textAnswer(provided.inputType() == AnswerInputType.TEXT ? provided.textAnswer() : null)
                                .voiceFileUrl(provided.inputType() == AnswerInputType.VOICE ? provided.voiceFileUrl() : null)
                                .voiceTranscript(provided.inputType() == AnswerInputType.VOICE ? provided.voiceTranscript() : null)
                                .evaluation(AnswerEvaluation.UNASSESSED)
                                .build()
                );
            } else {
                toSave.add(
                        TestAnswer.builder()
                                .attempt(attempt)
                                .question(q)
                                .inputType(null)
                                .textAnswer(null)
                                .voiceFileUrl(null)
                                .voiceTranscript(null)
                                .evaluation(AnswerEvaluation.UNASSESSED)
                                .build()
                );
            }
        }

        List<TestAnswer> savedAnswers = answerRepository.saveAll(toSave);

        final Long attemptId = attempt.getId();
        afterCommitExecutor.execute(() -> evaluationQueue.submitForEvaluation(attemptId));

        return mapper.toResponse(attempt, savedAnswers);
    }

    @Override
    @Transactional(readOnly = true)
    public TestAttemptResponse getLastAttempt(Long userId, Long testId) {
        TestAttempt attempt = attemptRepository.findFirstByUserIdAndTest_IdOrderByCreatedAtDesc(userId, testId)
                .orElseThrow(() -> new ResourceNotFoundException("No attempts for test: " + testId));
        List<TestAnswer> answers = answerRepository.findByAttemptIdWithQuestion(attempt.getId());
        return mapper.toResponse(attempt, answers);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<TestAttemptResponse> getAttempts(Long userId, Long testId, Pageable pageable) {
        Page<TestAttempt> attemptsPage = attemptRepository.findByUserIdAndTest_IdOrderByCreatedAtDesc(userId, testId, pageable);
        List<TestAttempt> attempts = attemptsPage.getContent();

        if (attempts.isEmpty()) {
            return Page.empty(pageable);
        }

        List<TestAnswer> allAnswers = answerRepository.findByAttemptInWithQuestion(attempts);
        Map<Long, List<TestAnswer>> answersByAttemptId = allAnswers.stream()
                .collect(Collectors.groupingBy(answer -> answer.getAttempt().getId()));

        return attemptsPage.map(attempt ->
                mapper.toResponse(attempt, answersByAttemptId.getOrDefault(attempt.getId(), Collections.emptyList()))
        );
    }

    /**
     * Присланные ответы должны соответствовать уникальному подмножеству вопросов теста.
     */
    private void validateAnswersSubset(List<AttemptAnswerCreate> answers, TestModel test) {
        if (answers == null || answers.isEmpty()) {
            return;
        }

        Set<Long> providedQIds = new HashSet<>();
        for (AttemptAnswerCreate a : answers) {
            if (!providedQIds.add(a.questionId())) {
                throw new ResourceConflictException("Duplicate questionId submitted: " + a.questionId());
            }
        }

        Set<Long> actualQIds = test.getQuestions().stream().map(Question::getId).collect(Collectors.toSet());
        if (!actualQIds.containsAll(providedQIds)) {
            Set<Long> unknownQIds = new HashSet<>(providedQIds);
            unknownQIds.removeAll(actualQIds);
            throw new ResourceConflictException("Request contains questionId(s) not belonging to this test: " + unknownQIds);
        }
    }

    private void validateInputCoherence(AttemptAnswerCreate a) {
        if (a.inputType() == AnswerInputType.TEXT) {
            if (isBlank(a.textAnswer()) || !isBlank(a.voiceFileUrl()) || !isBlank(a.voiceTranscript())) {
                throw new ResourceConflictException("TEXT requires textAnswer and no voice fields");
            }
        } else {
            if (isBlank(a.voiceFileUrl()) || !isBlank(a.textAnswer())) {
                throw new ResourceConflictException("VOICE requires voiceFileUrl and no textAnswer");
            }
        }
    }

    private Module resolveModule(TestModel test) {
        if (test.getModule() == null) {
            throw new ResourceConflictException("Test has no module");
        }
        return test.getModule();
    }

    private boolean isBlank(String s) {
        return s == null || s.trim().isEmpty();
    }
}
