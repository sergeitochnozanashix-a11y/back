package software.pxel.learneasy.service.async;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

@Component
@Slf4j
public class AIChatProcessingQueue {

    private final BlockingQueue<Long> queue = new LinkedBlockingQueue<>();

    public void submit(Long chatId) {
        if (chatId == null) {
            log.warn("Chat ID is null, skipping submission to processing queue.");
            return;
        }
        boolean success = queue.offer(chatId);
        if (success) {
            log.info("Chat with id=[{}] has been submitted for AI processing.", chatId);
        } else {
            log.error("Failed to submit Chat with id=[{}] to AI processing queue. The queue may be full.", chatId);
        }
    }

    public Long take() throws InterruptedException {
        return queue.take();
    }
}
