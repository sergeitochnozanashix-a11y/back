package software.pxel.learneasy.service.impl;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.core.io.InputStreamResource;
import software.amazon.awssdk.awscore.exception.AwsErrorDetails;
import software.amazon.awssdk.awscore.exception.AwsServiceException;
import software.amazon.awssdk.core.ResponseInputStream;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.*;
import software.pxel.learneasy.api.dto.s3.DownloadableFile;
import software.pxel.learneasy.exception.ResourceNotFoundException;
import software.pxel.learneasy.exception.StorageException;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class FileStorageServiceImplTest {

    @Mock
    private S3Client s3Client;

    @InjectMocks
    private FileStorageServiceImpl fileStorageService;

    private final String voiceBucketName = "voice-bucket";
    private final String mediaBucketName = "media-bucket";

    @BeforeEach
    void setUp() {
        fileStorageService = new FileStorageServiceImpl(s3Client, voiceBucketName, mediaBucketName);
    }

    @Test
    void init_ShouldAttemptToCreateBothBuckets() {
        when(s3Client.headBucket(any(HeadBucketRequest.class)))
                .thenThrow(NoSuchBucketException.builder().build());
        when(s3Client.createBucket(any(CreateBucketRequest.class)))
                .thenReturn(CreateBucketResponse.builder().build());

        fileStorageService.init();

        verify(s3Client, times(1)).headBucket(HeadBucketRequest.builder().bucket(voiceBucketName).build());
        verify(s3Client, times(1)).createBucket(CreateBucketRequest.builder().bucket(voiceBucketName).build());
        verify(s3Client, times(1)).headBucket(HeadBucketRequest.builder().bucket(mediaBucketName).build());
        verify(s3Client, times(1)).createBucket(CreateBucketRequest.builder().bucket(mediaBucketName).build());
    }

    @Test
    void createBucketIfNotExists_WhenBucketExists_ShouldNotCreate() {
        String bucketName = "existing-bucket";
        when(s3Client.headBucket(any(HeadBucketRequest.class))).thenReturn(HeadBucketResponse.builder().build());

        fileStorageService.createBucketIfNotExists(bucketName);

        verify(s3Client, times(1)).headBucket(any(HeadBucketRequest.class));
        verify(s3Client, never()).createBucket(any(CreateBucketRequest.class));
    }

    @Test
    void createBucketIfNotExists_WhenBucketNotExists_ShouldCreate() {
        String bucketName = "new-bucket";
        when(s3Client.headBucket(any(HeadBucketRequest.class)))
                .thenThrow(NoSuchBucketException.builder().build());
        when(s3Client.createBucket(any(CreateBucketRequest.class)))
                .thenReturn(CreateBucketResponse.builder().build());

        fileStorageService.createBucketIfNotExists(bucketName);

        verify(s3Client, times(1)).headBucket(any(HeadBucketRequest.class));
        verify(s3Client, times(1)).createBucket(any(CreateBucketRequest.class));
    }

    @Test
    void createBucketIfNotExists_WhenBucketCreationFails_ShouldThrowStorageException() {
        String bucketName = "problematic-bucket";
        when(s3Client.headBucket(any(HeadBucketRequest.class)))
                .thenThrow(NoSuchBucketException.builder().build());

        AwsServiceException s3Exception = S3Exception.builder()
                .statusCode(500)
                .awsErrorDetails(AwsErrorDetails.builder()
                        .errorMessage("Bucket creation failed")
                        .build())
                .build();

        when(s3Client.createBucket(any(CreateBucketRequest.class)))
                .thenThrow(s3Exception);

        StorageException exception = assertThrows(StorageException.class, () ->
                fileStorageService.createBucketIfNotExists(bucketName));

        assertTrue(exception.getMessage().contains("Failed to create bucket"));
    }

    @Test
    void uploadInputStream_ShouldUploadSuccessfully() throws IOException {
        String objectKey = "file.txt";
        String contentType = "text/plain";
        long contentLength = 10L;
        InputStream inputStream = new ByteArrayInputStream("test data".getBytes());
        when(s3Client.putObject(any(PutObjectRequest.class), any(RequestBody.class)))
                .thenReturn(PutObjectResponse.builder().build());

        assertDoesNotThrow(() ->
                fileStorageService.uploadInputStream(objectKey, inputStream, contentLength, contentType, voiceBucketName));

        verify(s3Client, times(1)).putObject(any(PutObjectRequest.class), any(RequestBody.class));
        inputStream.close();
    }

    @Test
    void uploadInputStream_WhenS3Error_ShouldThrowStorageException() {
        String objectKey = "file.txt";
        String contentType = "text/plain";
        long contentLength = 10L;
        InputStream inputStream = new ByteArrayInputStream("test data".getBytes());

        S3Exception s3Exception = (S3Exception) S3Exception.builder()
                .awsErrorDetails(AwsErrorDetails.builder()
                        .errorMessage("S3 error")
                        .build())
                .build();

        when(s3Client.putObject(any(PutObjectRequest.class), any(RequestBody.class)))
                .thenThrow(s3Exception);

        assertThrows(StorageException.class, () ->
                fileStorageService.uploadInputStream(objectKey, inputStream, contentLength, contentType, voiceBucketName));
    }

    @Test
    void uploadInputStream_WhenIOError_ShouldThrowStorageException() throws IOException {
        String objectKey = "file.txt";
        String contentType = "text/plain";
        long contentLength = 10L;

        InputStream failingInputStream = mock(InputStream.class);
        when(failingInputStream.read(any(byte[].class), anyInt(), anyInt()))
                .thenThrow(new IOException("Simulated IO error"));

        when(s3Client.putObject(any(PutObjectRequest.class), any(RequestBody.class)))
                .thenAnswer(invocation -> {
                    RequestBody body = invocation.getArgument(1);
                    try (InputStream stream = body.contentStreamProvider().newStream()) {
                        stream.read(new byte[10]);
                    }
                    return PutObjectResponse.builder().build();
                });

        StorageException exception = assertThrows(StorageException.class, () -> fileStorageService.uploadInputStream(
                objectKey,
                failingInputStream,
                contentLength,
                contentType,
                voiceBucketName
        ));

        assertTrue(exception.getMessage().contains("Failed to upload file"));
        assertNotNull(exception.getCause());
        assertInstanceOf(IOException.class, exception.getCause());

        verify(s3Client).putObject(any(PutObjectRequest.class), any(RequestBody.class));
    }

    @Test
    void downloadFile_ShouldReturnDownloadableFile() {
        String objectKey = "file.txt";
        String expectedFilename = "file.txt";
        String contentType = "text/plain";
        long contentLength = 10L;

        ResponseInputStream<GetObjectResponse> mockResponseInputStream = mock(ResponseInputStream.class);
        GetObjectResponse mockResponse = GetObjectResponse.builder()
                .contentType(contentType)
                .contentLength(contentLength)
                .build();

        when(mockResponseInputStream.response()).thenReturn(mockResponse);
        when(s3Client.getObject(any(GetObjectRequest.class)))
                .thenReturn(mockResponseInputStream);

        DownloadableFile result = fileStorageService.downloadFile(objectKey, mediaBucketName);

        assertNotNull(result);
        assertEquals(contentType, result.contentType());
        assertEquals(contentLength, result.contentLength());
        assertEquals(expectedFilename, result.filename());
        assertInstanceOf(InputStreamResource.class, result.resource());
    }

    @Test
    void downloadFile_WhenFileNotFound_ShouldThrowResourceNotFoundException() {
        String objectKey = "nonexistent/file.txt";
        when(s3Client.getObject(any(GetObjectRequest.class)))
                .thenThrow(NoSuchKeyException.builder().build());

        assertThrows(ResourceNotFoundException.class, () ->
                fileStorageService.downloadFile(objectKey, mediaBucketName));
    }

    @Test
    void downloadFile_WhenS3Error_ShouldThrowStorageException() {
        String objectKey = "file.txt";

        S3Exception s3Exception = (S3Exception) S3Exception.builder()
                .awsErrorDetails(AwsErrorDetails.builder()
                        .errorMessage("S3 error")
                        .build())
                .build();

        when(s3Client.getObject(any(GetObjectRequest.class)))
                .thenThrow(s3Exception);

        assertThrows(StorageException.class, () ->
                fileStorageService.downloadFile(objectKey, mediaBucketName));
    }

    @Test
    void downloadFile_WhenObjectKeyIsNull_ShouldReturnDefaultFilename() {
        String objectKey = null;
        String expectedFilename = "downloaded_file";
        String contentType = "application/octet-stream";
        long contentLength = 0L;

        ResponseInputStream<GetObjectResponse> mockResponseInputStream = mock(ResponseInputStream.class);
        GetObjectResponse mockResponse = GetObjectResponse.builder()
                .contentType(contentType)
                .contentLength(contentLength)
                .build();

        when(mockResponseInputStream.response()).thenReturn(mockResponse);
        when(s3Client.getObject(any(GetObjectRequest.class)))
                .thenReturn(mockResponseInputStream);

        DownloadableFile result = fileStorageService.downloadFile(objectKey, mediaBucketName);

        assertNotNull(result);
        assertEquals(expectedFilename, result.filename());
    }

    @Test
    void downloadFile_WhenObjectKeyIsEmpty_ShouldReturnEmptyFilename() {
        String objectKey = "";
        String expectedFilename = "";
        String contentType = "application/octet-stream";
        long contentLength = 0L;

        ResponseInputStream<GetObjectResponse> mockResponseInputStream = mock(ResponseInputStream.class);
        GetObjectResponse mockResponse = GetObjectResponse.builder()
                .contentType(contentType)
                .contentLength(contentLength)
                .build();

        when(mockResponseInputStream.response()).thenReturn(mockResponse);
        when(s3Client.getObject(any(GetObjectRequest.class)))
                .thenReturn(mockResponseInputStream);

        DownloadableFile result = fileStorageService.downloadFile(objectKey, mediaBucketName);

        assertNotNull(result);
        assertEquals(expectedFilename, result.filename());
    }
}
