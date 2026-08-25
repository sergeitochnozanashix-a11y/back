package software.pxel.learneasy.service;

import software.pxel.learneasy.api.dto.s3.DownloadableFile;
import software.pxel.learneasy.exception.ResourceNotFoundException;
import software.pxel.learneasy.exception.StorageException;

import java.io.InputStream;

public interface FileStorageService {

    void uploadInputStream(String objectKey, InputStream inputStream, long contentLength, String contentType, String bucketName)
            throws StorageException;

    DownloadableFile downloadFile(String objectKey, String bucketName)
            throws ResourceNotFoundException, StorageException;
}
