package software.pxel.learneasy.service.async;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

@Slf4j
@Component
public class AudioTranscriptionQueue {

    private final BlockingQueue<Long> queue = new LinkedBlockingQueue<>();

    public void submit(Long chatMessageId) {
        if (chatMessageId == null) {
            log.warn("Chat Message ID is null, skipping submission to transcription queue.");
            return;
        }

        boolean success = queue.offer(chatMessageId);

        if (success) {
            log.info("Message with id=[{}] has been submitted for audio transcription.", chatMessageId);
        } else {
            log.error("Failed to submit Message with id=[{}] to transcription queue.", chatMessageId);
        }
    }

    public Long take() throws InterruptedException {
        return queue.take();
    }
}
