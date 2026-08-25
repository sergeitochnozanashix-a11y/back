package software.pxel.learneasy.feign.whisper.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import software.pxel.learneasy.feign.whisper.dto.TranscriptionResponse;

@FeignClient(name = "whisperApiClient", url = "${feign.client.config.whisperApiClient.url}")
public interface WhisperApiClient {

    @PostMapping("/api/v1/speech-to-text")
    TranscriptionResponse transcribe(@RequestParam("url") String fileUrl);
}
