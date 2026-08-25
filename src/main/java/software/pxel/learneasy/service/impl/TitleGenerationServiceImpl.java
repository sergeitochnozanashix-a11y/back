package software.pxel.learneasy.service.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import software.pxel.learneasy.service.async.TitleGenerationQueue;
import software.pxel.learneasy.service.async.TitleGenerationService;
import software.pxel.learneasy.service.async.TitleGeneratorProcessor;

@Service
@RequiredArgsConstructor
@Slf4j
public class TitleGenerationServiceImpl implements TitleGenerationService {

    private final TitleGenerationQueue titleGenerationQueue;
    private final TitleGeneratorProcessor processor;

    @Override
    @Async("ioExecutor")
    public void startProcessingLoop() {
        log.info("Title generation loop started.");
        while (!Thread.currentThread().isInterrupted()) {
            try {
                Long chatId = titleGenerationQueue.take();
                log.info("Took Chat with id=[{}] from title generation queue.", chatId);
                processor.processTitleGeneration(chatId);
            } catch (InterruptedException e) {
                log.warn("Title generation loop was interrupted.");
                Thread.currentThread().interrupt();
            } catch (Exception e) {
                log.error("An unexpected error occurred during title generation loop.", e);
            }
        }
    }
}
