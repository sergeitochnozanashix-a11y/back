package software.pxel.learneasy.service.async;

import lombok.RequiredArgsConstructor;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class AIChatProcessingLoopRunner implements CommandLineRunner {

    private final AIChatProcessingService aiChatProcessingService;

    @Override
    public void run(String... args) {
        aiChatProcessingService.startProcessingLoop();
    }
}
