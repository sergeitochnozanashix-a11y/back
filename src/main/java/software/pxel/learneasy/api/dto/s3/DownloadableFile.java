package software.pxel.learneasy.api.dto.s3;

import org.springframework.core.io.Resource;

public record DownloadableFile(
        Resource resource,
        String contentType,
        long contentLength,
        String filename
) {
}
