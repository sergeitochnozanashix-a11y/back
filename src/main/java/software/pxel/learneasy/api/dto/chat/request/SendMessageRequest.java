package software.pxel.learneasy.api.dto.chat.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import org.hibernate.validator.constraints.URL;
import software.pxel.learneasy.api.validation.AtLeastOneFieldNotEmpty;

import java.util.List;

@Schema(description = "Запрос на отправку сообщения")
@AtLeastOneFieldNotEmpty(
        fields = {"text", "audioFileUrl"},
        message = "Either text or audioFileUrl must be provided"
)
public record SendMessageRequest(

        @Schema(
                description = "Текст сообщения. Необязательно, если указан audioFileUrl.",
                example = "Посмотри, что я там наговорил!?"
        )
        String text,

        @Schema(
                description = "Опциональный URL для прикрепленного аудиофайла.",
                example = "https://your-minio.com/bucket/audio.mp3"
        )
        @URL(message = "Audio file URL должен быть валидным URL")
        String audioFileUrl,

        @Schema(
                description = """
                        Опциональный список метаданных для прикрепленных файлов.
                        Файлы должны быть предварительно загружены в хранилище (например, Minio).
                        """,
                example = """
                        [
                          {
                             "downloadUrl": "https://api.edu.pxel.software/api/v1/file-storage/download/1760349271824_55d0dd4f-e56d-4b9d-8590-ea24a4012a3d.mp3",
                             "originalFilename": "audio5_dDjeYAgX.mp3",
                             "contentType": "audio/mpeg",
                             "size": 2402264
                           }
                        ]
                        """
        )
        @Valid List<AttachmentRequestDto> attachments
) {
}
