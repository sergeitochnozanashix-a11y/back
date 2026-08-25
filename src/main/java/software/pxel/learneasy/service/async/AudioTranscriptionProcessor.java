package software.pxel.learneasy.service.async;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import software.pxel.learneasy.feign.ai.dto.AudioTranscriptionResponse;
import software.pxel.learneasy.feign.ai.service.AIGatewayService;

@Slf4j
@Service
@RequiredArgsConstructor
public class AudioTranscriptionProcessor {

    private final AIGatewayService aiGatewayService;
    private final TranscriptionPersistenceHelper persistenceHelper;

    public static final String MODEL_TRANSCRIPTION_REQUEST = "gpt-4o-transcribe";
    public static final String INSTRUCTION_TRANSCRIPTION_REQUEST = "Сохраняй числа и имена собственные";

    public void processTranscription(Long chatMessageId) {
        try {
            TranscriptionPersistenceHelper.TranscriptionData data =
                    persistenceHelper.prepareTranscriptionData(chatMessageId);

            if (data == null) {
                log.warn("No audio attachment found for message ID: {}", chatMessageId);
                return;
            }

            log.info("Starting transcription for attachment of message [{}].", chatMessageId);
            AudioTranscriptionResponse response = aiGatewayService.transcribeMp3ByUrl(
                    data.downloadUrl(), MODEL_TRANSCRIPTION_REQUEST, INSTRUCTION_TRANSCRIPTION_REQUEST
            );

            persistenceHelper.saveTranscriptionResult(chatMessageId, response.text());

        } catch (Exception e) {
            log.error("Failed to process transcription for message ID: {}. Reason: {}",
                    chatMessageId, e.getMessage(), e);
        }
    }
}
