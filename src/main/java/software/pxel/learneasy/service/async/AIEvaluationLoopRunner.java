package software.pxel.learneasy.service.async;

import lombok.RequiredArgsConstructor;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class AIEvaluationLoopRunner implements CommandLineRunner {

    private final AIEvaluationService aiEvaluationService;

    @Override
    public void run(String... args) {
        aiEvaluationService.startEvaluationLoop();
    }
}
