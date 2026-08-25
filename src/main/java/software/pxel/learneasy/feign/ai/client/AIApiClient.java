package software.pxel.learneasy.feign.ai.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import software.pxel.learneasy.feign.ai.config.AIFeignConfig;
import software.pxel.learneasy.feign.ai.dto.AudioTranscriptionRequest;
import software.pxel.learneasy.feign.ai.dto.AudioTranscriptionResponse;
import software.pxel.learneasy.feign.ai.dto.ChatCompletionsRequest;
import software.pxel.learneasy.feign.ai.dto.ChatCompletionsResponse;
import software.pxel.learneasy.feign.ai.dto.ResponsesRequest;
import software.pxel.learneasy.feign.ai.dto.ResponsesResponse;

@FeignClient(
        name = "aiApiClient",
        url = "${feign.client.config.aiApiClient.url}",
        configuration = AIFeignConfig.class
)
public interface AIApiClient {

    @PostMapping(path = "/api/ai/transcriptions", consumes = MediaType.APPLICATION_JSON_VALUE)
    AudioTranscriptionResponse transcribe(@RequestBody AudioTranscriptionRequest request);

    @PostMapping(path = "/api/ai/responses", consumes = MediaType.APPLICATION_JSON_VALUE)
    ResponsesResponse responses(@RequestBody ResponsesRequest request);

    @PostMapping(path = "/api/ai/chat/completions", consumes = MediaType.APPLICATION_JSON_VALUE)
    ChatCompletionsResponse getChatCompletion(@RequestBody ChatCompletionsRequest request);
}
