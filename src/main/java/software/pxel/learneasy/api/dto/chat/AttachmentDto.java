package software.pxel.learneasy.api.dto.chat;

public record AttachmentDto(
        Long id,
        String downloadUrl,
        String originalFilename,
        String contentType,
        Long size
) {
}
