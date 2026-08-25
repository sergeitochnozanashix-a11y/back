package software.pxel.learneasy.service.async;

import lombok.RequiredArgsConstructor;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class TitleGenerationLoopRunner implements CommandLineRunner {

    private final TitleGenerationService titleGenerationService;

    @Override
    public void run(String... args) {
        titleGenerationService.startProcessingLoop();
    }
}
