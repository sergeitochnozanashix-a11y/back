package software.pxel.learneasy.feign.whisper.service;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import software.pxel.learneasy.feign.whisper.client.WhisperApiClient;
import software.pxel.learneasy.feign.whisper.dto.TranscriptionResponse;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.eq;

class WhisperGatewayServiceTest {

    @Test
    @DisplayName("transcribeByUrl: happy path")
    void transcribe_ok() {
        WhisperApiClient client = Mockito.mock(WhisperApiClient.class);
        Mockito.when(client.transcribe(eq("https://example.com/a.mp3")))
                .thenReturn(new TranscriptionResponse("hello", 18846));

        WhisperGatewayService svc = new WhisperGatewayService(client);
        var resp = svc.transcribeByUrl("https://example.com/a.mp3");

        assertNotNull(resp);
        assertEquals("hello", resp.text());
        assertEquals(18846, resp.durationMs());
    }

    @Test
    @DisplayName("transcribeByUrl: пустой url -> IllegalArgumentException")
    void transcribe_blankUrl() {
        WhisperApiClient client = Mockito.mock(WhisperApiClient.class);
        WhisperGatewayService svc = new WhisperGatewayService(client);
        assertThrows(IllegalArgumentException.class, () -> svc.transcribeByUrl(" "));
    }

    @Test
    @DisplayName("transcribeByUrl: неподдерживаемая схема -> IllegalArgumentException")
    void transcribe_badScheme() {
        WhisperApiClient client = Mockito.mock(WhisperApiClient.class);
        WhisperGatewayService svc = new WhisperGatewayService(client);
        assertThrows(IllegalArgumentException.class, () -> svc.transcribeByUrl("file:///tmp/a.mp3"));
    }
}
