package software.pxel.learneasy.feign.ai.service;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import software.pxel.learneasy.feign.ai.client.AIApiClient;
import software.pxel.learneasy.feign.ai.dto.AudioTranscriptionRequest;
import software.pxel.learneasy.feign.ai.dto.AudioTranscriptionResponse;
import software.pxel.learneasy.feign.ai.dto.ResponsesRequest;
import software.pxel.learneasy.feign.ai.dto.ResponsesResponse;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;

class AIGatewayServiceTest {

    @Test
    @DisplayName("transcribeMp3ByUrl: happy path")
    void transcribe_ok() {
        AIApiClient client = Mockito.mock(AIApiClient.class);
        Mockito.when(client.transcribe(any(AudioTranscriptionRequest.class)))
                .thenReturn(new AudioTranscriptionResponse("привет"));

        AIGatewayService svc = new AIGatewayService(client);
        var resp = svc.transcribeMp3ByUrl("https://example.com/a.mp3");

        assertNotNull(resp);
        assertEquals("привет", resp.text());
    }

    @Test
    @DisplayName("transcribeMp3ByUrl: пустой url -> IllegalArgumentException")
    void transcribe_blankUrl() {
        AIApiClient client = Mockito.mock(AIApiClient.class);
        AIGatewayService svc = new AIGatewayService(client);
        assertThrows(IllegalArgumentException.class, () -> svc.transcribeMp3ByUrl(" "));
    }

    @Test
    @DisplayName("transcribeMp3ByUrl: неподдерживаемая схема -> IllegalArgumentException")
    void transcribe_badScheme() {
        AIApiClient client = Mockito.mock(AIApiClient.class);
        AIGatewayService svc = new AIGatewayService(client);
        assertThrows(IllegalArgumentException.class, () -> svc.transcribeMp3ByUrl("file:///tmp/a.mp3"));
    }

    @Test
    @DisplayName("transcribeMp3ByUrl: не mp3 -> IllegalArgumentException")
    void transcribe_notMp3() {
        AIApiClient client = Mockito.mock(AIApiClient.class);
        AIGatewayService svc = new AIGatewayService(client);
        assertThrows(IllegalArgumentException.class, () -> svc.transcribeMp3ByUrl("https://ex.com/a.wav"));
    }

    @Test
    @DisplayName("responses: проксирование запроса")
    void responses_ok() {
        AIApiClient client = Mockito.mock(AIApiClient.class);
        ResponsesResponse.Content c = new ResponsesResponse.Content("output_text", "ok");
        ResponsesResponse.Message m = new ResponsesResponse.Message("m1", "message", "assistant", List.of(c));
        ResponsesResponse expected = new ResponsesResponse("id", "responses", 0L, "gpt", "completed", List.of(m), "ok", null);
        Mockito.when(client.responses(any(ResponsesRequest.class))).thenReturn(expected);

        AIGatewayService svc = new AIGatewayService(client);
        var out = svc.responses(new ResponsesRequest("gpt-5-mini", "ru", "ping"));

        assertNotNull(out);
        assertEquals("ok", out.outputText());
        assertEquals("completed", out.status());
    }
}
