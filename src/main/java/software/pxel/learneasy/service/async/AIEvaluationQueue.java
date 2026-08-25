package software.pxel.learneasy.service.async;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

/**
 * Это простая in-memory очередь.
 * Она не персистентна и не подходит для работы с несколькими инстансами.
 * Все задачи будут утеряны при перезапуске приложения.
 * При усложнении структуры проекта следует заменить на RabbitMQ/Kafka.
 */
@Component
@Slf4j
public class AIEvaluationQueue {

    private final BlockingQueue<Long> queue = new LinkedBlockingQueue<>();

    public void submitForEvaluation(Long attemptId) {
        if (attemptId == null) {
            log.warn("AttemptId is null, skipping submission to evaluation queue.");
            return;
        }
        boolean success = queue.offer(attemptId);
        if (success) {
            log.info("TestAttempt with id=[{}] has been submitted for AI evaluation.", attemptId);
        } else {
            log.error("Failed to submit TestAttempt with id=[{}] to AI evaluation queue. The queue may be full.", attemptId);
        }
    }

    public Long take() throws InterruptedException {
        return queue.take();
    }
}
