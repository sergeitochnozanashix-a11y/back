package software.pxel.learneasy.feign.ai.service;

import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import software.pxel.learneasy.feign.ai.client.AIApiClient;
import software.pxel.learneasy.feign.ai.dto.AudioTranscriptionRequest;
import software.pxel.learneasy.feign.ai.dto.AudioTranscriptionResponse;
import software.pxel.learneasy.feign.ai.dto.ChatCompletionsRequest;
import software.pxel.learneasy.feign.ai.dto.ChatCompletionsResponse;
import software.pxel.learneasy.feign.ai.dto.ResponsesRequest;
import software.pxel.learneasy.feign.ai.dto.ResponsesResponse;

import java.net.URI;

@Service
public class AIGatewayService {

    private final AIApiClient client;

    public AIGatewayService(AIApiClient client) {
        this.client = client;
    }

    public AudioTranscriptionResponse transcribeMp3ByUrl(String url) {
        validateUrl(url);
        return client.transcribe(new AudioTranscriptionRequest(url, null, null));
    }

    public AudioTranscriptionResponse transcribeMp3ByUrl(String url, String model, String instructions) {
        validateUrl(url);
        return client.transcribe(new AudioTranscriptionRequest(url, model, instructions));
    }

    public ResponsesResponse responses(ResponsesRequest request) {
        return client.responses(request);
    }

    public ChatCompletionsResponse getChatCompletion(ChatCompletionsRequest request) {
        return client.getChatCompletion(request);
    }

    private void validateUrl(String url) {
        if (!StringUtils.hasText(url)) {
            throw new IllegalArgumentException("url must not be blank");
        }
        URI uri = URI.create(url);
        String scheme = uri.getScheme() == null ? "" : uri.getScheme().toLowerCase();
        if (!scheme.equals("http") && !scheme.equals("https")) {
            throw new IllegalArgumentException("only http/https are supported");
        }
        String path = uri.getPath() == null ? "" : uri.getPath().toLowerCase();
        if (!path.endsWith(".mp3")) {
            throw new IllegalArgumentException("only .mp3 files are supported");
        }
    }
}
