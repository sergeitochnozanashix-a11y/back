package software.pxel.learneasy.feign.whisper.service;

import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import software.pxel.learneasy.feign.whisper.client.WhisperApiClient;
import software.pxel.learneasy.feign.whisper.dto.TranscriptionResponse;

import java.net.URI;

@Service
public class WhisperGatewayService {

    private final WhisperApiClient client;

    public WhisperGatewayService(WhisperApiClient client) {
        this.client = client;
    }

    public TranscriptionResponse transcribeByUrl(String url) {
        validate(url);
        return client.transcribe(url);
    }

    private void validate(String url) {
        if (!StringUtils.hasText(url)) {
            throw new IllegalArgumentException("url must not be blank");
        }
        URI uri = URI.create(url);
        String scheme = uri.getScheme() == null ? "" : uri.getScheme().toLowerCase();
        if (!scheme.equals("http") && !scheme.equals("https")) {
            throw new IllegalArgumentException("only http/https are supported");
        }
    }
}
