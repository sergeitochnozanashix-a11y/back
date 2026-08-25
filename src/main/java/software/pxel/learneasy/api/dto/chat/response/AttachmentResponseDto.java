package software.pxel.learneasy.api.dto.chat.response;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Метаданные одного прикрепленного файла в ответе")
public record AttachmentResponseDto(
        @Schema(description = "ID вложения")
        Long id,

        @Schema(description = "Прямая ссылка для скачивания файла")
        String downloadUrl,

        @Schema(description = "Оригинальное имя файла")
        String originalFilename,

        @Schema(description = "MIME-тип файла")
        String contentType,

        @Schema(description = "Размер файла в байтах")
        Long size
) {
}
