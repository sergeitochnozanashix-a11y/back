package software.pxel.learneasy.service.impl;

import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.core.io.InputStreamResource;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.core.ResponseInputStream;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.*;
import software.pxel.learneasy.api.dto.s3.DownloadableFile;
import software.pxel.learneasy.exception.ResourceNotFoundException;
import software.pxel.learneasy.exception.StorageException;
import software.pxel.learneasy.service.FileStorageService;

import java.io.IOException;
import java.io.InputStream;

@Slf4j
@Service
public class FileStorageServiceImpl implements FileStorageService {

    private final S3Client s3Client;
    private final String voiceBucketName;
    private final String mediaBucketName;

    public FileStorageServiceImpl(S3Client s3Client,
                                  @Qualifier("minioVoiceBucketName") String voiceBucketName,
                                  @Qualifier("minioMediaBucketName") String mediaBucketName) {
        this.s3Client = s3Client;
        this.voiceBucketName = voiceBucketName;
        this.mediaBucketName = mediaBucketName;
    }

    @PostConstruct
    public void init() {
        createBucketIfNotExists(this.voiceBucketName);
        createBucketIfNotExists(this.mediaBucketName);
    }

    public void createBucketIfNotExists(String bucketToCreate) {
        try {
            s3Client.headBucket(HeadBucketRequest.builder().bucket(bucketToCreate).build());
            log.info("Bucket '{}' already exists.", bucketToCreate);
        } catch (NoSuchBucketException e) {
            log.info("Bucket '{}' does not exist. Creating...", bucketToCreate);
            try {
                s3Client.createBucket(CreateBucketRequest.builder().bucket(bucketToCreate).build());
                log.info("Bucket '{}' created successfully.", bucketToCreate);
            } catch (S3Exception s3Ex) {
                log.error("Could not create bucket '{}': {}", bucketToCreate, s3Ex.awsErrorDetails().errorMessage(), s3Ex);
                if (s3Ex.statusCode() == 409) {
                    log.info("Bucket '{}' likely already exists (conflict).", bucketToCreate);
                } else {
                    throw new StorageException("Failed to create bucket " + bucketToCreate, s3Ex);
                }
            }
        } catch (S3Exception e) {
            log.error("Error checking or creating bucket '{}': {}", bucketToCreate, e.awsErrorDetails().errorMessage(), e);
            throw new StorageException("Error during bucket existence check or creation for " + bucketToCreate, e);
        }
    }

    @Override
    public void uploadInputStream(String objectKey, InputStream inputStream, long contentLength, String contentType, String bucketName) throws StorageException {
        PutObjectRequest putObjectRequest = PutObjectRequest.builder()
                .bucket(bucketName)
                .key(objectKey)
                .contentType(contentType)
                .contentLength(contentLength)
                .build();
        try (InputStream closableInputStream = inputStream) {
            s3Client.putObject(putObjectRequest, RequestBody.fromInputStream(closableInputStream, contentLength));
            log.info("InputStream uploaded successfully as '{}' to bucket '{}'. ContentType: {}, Length: {}", objectKey, bucketName, contentType, contentLength);
        } catch (IOException e) {
            log.error("IOException during S3 PutObject for key '{}': {}", objectKey, e.getMessage(), e);
            throw new StorageException("Failed to upload file due to IO error: " + objectKey, e);
        } catch (S3Exception e) {
            log.error("S3Exception during S3 PutObject for key '{}': {}", objectKey, e.awsErrorDetails().errorMessage(), e);
            throw new StorageException("S3_ERROR: Failed to upload file: " + objectKey, e);
        }
    }

    @Override
    public DownloadableFile downloadFile(String objectKey, String bucketName) throws ResourceNotFoundException, StorageException {
        GetObjectRequest getObjectRequest = GetObjectRequest.builder()
                .bucket(bucketName)
                .key(objectKey)
                .build();
        try {
            ResponseInputStream<GetObjectResponse> s3ObjectStream = s3Client.getObject(getObjectRequest);
            GetObjectResponse s3ObjectResponse = s3ObjectStream.response();

            String contentType = s3ObjectResponse.contentType();
            long contentLength = s3ObjectResponse.contentLength();
            String filename = extractFilenameFromObjectKey(objectKey);

            Resource resource = new InputStreamResource(s3ObjectStream);
            log.info("File '{}' prepared for download from bucket '{}'. ContentType: {}, Length: {}", objectKey, bucketName, contentType, contentLength);
            return new DownloadableFile(resource, contentType, contentLength, filename);

        } catch (NoSuchKeyException e) {
            log.warn("S3 Object not found for key '{}' in bucket '{}'", objectKey, bucketName, e);
            throw new ResourceNotFoundException("File not found: " + objectKey, e);
        } catch (S3Exception e) {
            log.error("S3Exception during S3 GetObject for key '{}': {}", objectKey, e.awsErrorDetails().errorMessage(), e);
            throw new StorageException("S3_ERROR: Failed to download file: " + objectKey, e);
        }
    }

    private String extractFilenameFromObjectKey(String objectKey) {
        if (objectKey == null) {
            return "downloaded_file";
        }
        int lastSlash = objectKey.lastIndexOf('/');
        if (lastSlash != -1 && lastSlash < objectKey.length() - 1) {
            return objectKey.substring(lastSlash + 1);
        }
        return objectKey;
    }
}
