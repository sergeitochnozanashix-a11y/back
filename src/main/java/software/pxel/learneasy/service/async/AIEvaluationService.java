package software.pxel.learneasy.service.async;

public interface AIEvaluationService {
    void startEvaluationLoop();

    void evaluateAttempt(Long attemptId);
}
