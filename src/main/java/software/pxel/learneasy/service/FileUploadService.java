package software.pxel.learneasy.service;

import org.springframework.web.multipart.MultipartFile;
import software.pxel.learneasy.api.dto.s3.DownloadableFile;
import software.pxel.learneasy.api.dto.s3.FileUploadResponse;

public interface FileUploadService {

    FileUploadResponse uploadFile(MultipartFile file);

    DownloadableFile downloadFile(String objectKey);
}
