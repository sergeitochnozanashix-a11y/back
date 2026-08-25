package software.pxel.learneasy.service.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import software.pxel.learneasy.service.async.AIEvaluationQueue;
import software.pxel.learneasy.service.async.AIEvaluationService;
import software.pxel.learneasy.service.async.AttemptProcessor;

@Service
@RequiredArgsConstructor
@Slf4j
public class AIEvaluationServiceImpl implements AIEvaluationService {

    private final AIEvaluationQueue evaluationQueue;
    private final AttemptProcessor attemptProcessor;

    @Override
    @Async
    public void startEvaluationLoop() {
        log.info("AI evaluation loop started.");
        while (!Thread.currentThread().isInterrupted()) {
            try {
                Long attemptId = evaluationQueue.take();
                log.info("Took TestAttempt with id=[{}] from evaluation queue.", attemptId);
                attemptProcessor.evaluateAttempt(attemptId);
            } catch (InterruptedException e) {
                log.warn("AI evaluation loop was interrupted.");
                Thread.currentThread().interrupt();
            } catch (Exception e) {
                log.error("An unexpected error occurred during AI evaluation loop.", e);
            }
        }
    }

    @Override
    public void evaluateAttempt(Long attemptId) {
        attemptProcessor.evaluateAttempt(attemptId);
    }
}
