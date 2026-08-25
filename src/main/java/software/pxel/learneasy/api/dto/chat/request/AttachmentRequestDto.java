package software.pxel.learneasy.api.dto.chat.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

@Schema(description = "Метаданные одного прикрепленного файла")
public record AttachmentRequestDto(

        //TODO 256
        @NotBlank @Size(max = 2048)
        @Schema(description = "Прямая ссылка для скачивания файла", requiredMode = Schema.RequiredMode.REQUIRED)
        String downloadUrl,

        @NotBlank @Size(max = 255)
        @Schema(description = "Оригинальное имя файла", requiredMode = Schema.RequiredMode.REQUIRED)
        String originalFilename,

        @NotBlank @Size(max = 128)
        @Schema(description = "MIME-тип файла", example = "application/pdf", requiredMode = Schema.RequiredMode.REQUIRED)
        String contentType,

        @NotNull
        @Schema(description = "Размер файла в байтах", requiredMode = Schema.RequiredMode.REQUIRED)
        Long size
) {
}
