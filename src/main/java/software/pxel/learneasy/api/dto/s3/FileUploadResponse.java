package software.pxel.learneasy.api.dto.s3;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Модель ответа после успешной загрузки файла")
public record FileUploadResponse(
        @Schema(description = "Сообщение о результате операции", example = "File uploaded successfully!") String message,
        @Schema(description = "Ссылка на скачивание файла", example = "https://domain.com/uri/download/1678886400000_unique_id.mp3") String downloadUrl,
        @Schema(description = "Оригинальное имя файла", example = "my_voice_memo.mp3") String originalFilename,
        @Schema(description = "MIME-тип файла", example = "audio/mpeg") String contentType,
        @Schema(description = "Размер файла в байтах", example = "1024768") long size
) {
}
