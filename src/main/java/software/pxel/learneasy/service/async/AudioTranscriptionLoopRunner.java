package software.pxel.learneasy.service.async;

import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;

@Slf4j
@Component
@RequiredArgsConstructor
public class AudioTranscriptionLoopRunner {

    private final Executor ioExecutor;
    private final AudioTranscriptionQueue queue;
    private final AudioTranscriptionProcessor processor;

    @PostConstruct
    public void startLoop() {
        ioExecutor.execute(() -> {
            log.info("Audio Transcription Loop started.");
            while (!Thread.currentThread().isInterrupted()) {
                try {
                    Long chatMessageId = queue.take();
                    log.info("Took message ID [{}] from transcription queue.", chatMessageId);

                    // Запускаем обработку в отдельном потоке
                    CompletableFuture.runAsync(() -> {
                        try {
                            processor.processTranscription(chatMessageId);
                        } catch (Exception e) {
                            log.error("Error processing transcription for message ID: {}", chatMessageId, e);
                        }
                    }, ioExecutor);

                } catch (InterruptedException e) {
                    log.warn("Audio Transcription Loop was interrupted.");
                    Thread.currentThread().interrupt();
                    break;
                } catch (Exception e) {
                    log.error("Unexpected error in Audio Transcription Loop", e);
                }
            }
        });
    }
}
