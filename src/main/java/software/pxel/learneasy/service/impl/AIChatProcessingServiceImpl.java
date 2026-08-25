package software.pxel.learneasy.service.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import software.pxel.learneasy.service.async.AIChatProcessingQueue;
import software.pxel.learneasy.service.async.AIChatProcessingService;
import software.pxel.learneasy.service.async.AIChatProcessor;

@Service
@RequiredArgsConstructor
@Slf4j
public class AIChatProcessingServiceImpl implements AIChatProcessingService {

    private final AIChatProcessingQueue processingQueue;
    private final AIChatProcessor processor;

    @Override
    @Async
    public void startProcessingLoop() {
        log.info("AI chat processing loop started.");
        while (!Thread.currentThread().isInterrupted()) {
            try {
                Long chatId = processingQueue.take();
                log.info("Took Chat with id=[{}] from processing queue.", chatId);
                processor.processMessage(chatId);
            } catch (InterruptedException e) {
                log.warn("AI chat processing loop was interrupted.");
                Thread.currentThread().interrupt();
            } catch (Exception e) {
                log.error("An unexpected error occurred during AI chat processing loop.", e);
            }
        }
    }
}
