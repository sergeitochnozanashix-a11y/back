package software.pxel.learneasy.util;

import jakarta.annotation.PostConstruct;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

@Service
@Slf4j
@Getter
public class PromptManager {

    private static final String ASSESSMENT_INSTRUCTIONS_PATH = "/prompts/ai-assessment-instructions.txt";
    private static final String CHAT_SYSTEM_PROMPT_PATH = "/prompts/ai-chat-system-prompt.txt";
    private static final String CHAT_TITLE_SYSTEM_PROMPT_PATH = "/prompts/ai-chat-title-system-prompt.txt";

    private String assessmentInstructions;
    private String chatSystemPrompt;
    private String chatTitleSystemPrompt;

    @PostConstruct
    private void initialize() {
        log.info("Loading AI prompts from resources...");
        assessmentInstructions = loadPromptFromResource(ASSESSMENT_INSTRUCTIONS_PATH);
        chatSystemPrompt = loadPromptFromResource(CHAT_SYSTEM_PROMPT_PATH);
        chatTitleSystemPrompt = loadPromptFromResource(CHAT_TITLE_SYSTEM_PROMPT_PATH);
        log.info("AI prompts loaded successfully.");
    }

    private String loadPromptFromResource(String path) {
        try (InputStream inputStream = PromptManager.class.getResourceAsStream(path)) {
            if (inputStream == null) {
                log.error("Resource file not found: {}", path);
                throw new IllegalStateException("Could not find prompt file in resources: " + path);
            }
            return new String(inputStream.readAllBytes(), StandardCharsets.UTF_8);
        } catch (IOException e) {
            log.error("Failed to read prompt file: {}", path, e);
            throw new IllegalStateException("Failed to read prompt file: " + path, e);
        }
    }
}
