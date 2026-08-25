package software.pxel.learneasy.service.impl;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.MockedConstruction;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;
import software.pxel.learneasy.api.dto.s3.DownloadableFile;
import software.pxel.learneasy.api.dto.s3.FileUploadResponse;
import software.pxel.learneasy.exception.BadRequestException;
import software.pxel.learneasy.exception.ResourceNotFoundException;
import software.pxel.learneasy.exception.StorageException;
import software.pxel.learneasy.service.FileStorageService;
import software.pxel.learneasy.service.FileSupportService;
import ws.schild.jave.Encoder;
import ws.schild.jave.EncoderException;
import ws.schild.jave.MultimediaObject;
import ws.schild.jave.encode.EncodingAttributes;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class FileUploadServiceImplTest {

    @Mock
    private FileStorageService fileStorageService;

    @Mock
    private FileSupportService fileSupportService;

    @Mock
    private Authentication authentication;

    @Mock
    private SecurityContext securityContext;

    private FileUploadServiceImpl fileUploadService;

    private final String voiceBucketName = "voice-test-bucket";
    private final String mediaBucketName = "media-test-bucket";
    private final String testUsername = "testUser";

    @BeforeEach
    void setUp() {
        when(fileSupportService.getParsableFileExtensions()).thenReturn(Set.of("txt", "csv", "md", "java"));

        fileUploadService = new FileUploadServiceImpl(fileStorageService, fileSupportService, voiceBucketName, mediaBucketName);

        ReflectionTestUtils.invokeMethod(fileUploadService, "initializeSupportedExtensions");

        mockAuthentication(testUsername);
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    private void mockAuthentication(String username) {
        lenient().when(securityContext.getAuthentication()).thenReturn(authentication);
        SecurityContextHolder.setContext(securityContext);
        lenient().when(authentication.getName()).thenReturn(username);
    }

    // ----------------------- УСПЕШНЫЕ ЗАГРУЗКИ -----------------------

    @Test
    @DisplayName("uploadFile — успех: аудио (.mp3) уходит в voiceBucket, URL оканчивается на .mp3")
    void uploadFile_ShouldUploadAudioSuccessfullyAndReturnResponse() {
        MultipartFile audioFile = new MockMultipartFile(
                "audio.mp3", "audio.mp3", "audio/mpeg", "audio data".getBytes()
        );

        // захват аргумента path(...) у билдера
        AtomicReference<String> capturedPath = new AtomicReference<>("");

        // Мокаем статический билдер URI
        try (MockedStatic<ServletUriComponentsBuilder> mocked = mockStatic(ServletUriComponentsBuilder.class)) {
            ServletUriComponentsBuilder builder = mock(ServletUriComponentsBuilder.class, RETURNS_SELF);
            mocked.when(ServletUriComponentsBuilder::fromCurrentContextPath).thenReturn(builder);

            // фиксируем path-аргумент
            when(builder.path(anyString())).thenAnswer(inv -> {
                capturedPath.set(inv.getArgument(0));
                return builder;
            });
            when(builder.toUriString()).thenAnswer(inv -> "http://host" + capturedPath.get());
            doNothing().when(fileStorageService).uploadInputStream(anyString(), any(InputStream.class), anyLong(), anyString(), eq(voiceBucketName));

            FileUploadResponse response = fileUploadService.uploadFile(audioFile);

            assertNotNull(response);
            assertTrue(response.message().toLowerCase().contains("uploaded successfully"));
            assertTrue(response.downloadUrl().endsWith(".mp3"));
            // убеждаемся, что именно path(...) сформирован с .mp3
            assertTrue(capturedPath.get().endsWith(".mp3"));

            assertEquals(audioFile.getOriginalFilename(), response.originalFilename());
            assertEquals(audioFile.getContentType(), response.contentType());
            assertEquals(audioFile.getSize(), response.size());

            verify(fileStorageService, times(1)).uploadInputStream(
                    anyString(), any(InputStream.class), eq(audioFile.getSize()), eq(audioFile.getContentType()), eq(voiceBucketName));
        }
    }

    @Test
    @DisplayName("uploadFile — успех: медиа (.jpg) уходит в mediaBucket, URL оканчивается на .jpg")
    void uploadFile_ShouldUploadMediaSuccessfullyAndReturnResponse() {
        MultipartFile imageFile = new MockMultipartFile(
                "image.jpg", "image.jpg", "image/jpeg", "image data".getBytes()
        );

        AtomicReference<String> capturedPath = new AtomicReference<>("");

        try (MockedStatic<ServletUriComponentsBuilder> mocked = mockStatic(ServletUriComponentsBuilder.class)) {
            ServletUriComponentsBuilder builder = mock(ServletUriComponentsBuilder.class, RETURNS_SELF);
            mocked.when(ServletUriComponentsBuilder::fromCurrentContextPath).thenReturn(builder);

            when(builder.path(anyString())).thenAnswer(inv -> {
                capturedPath.set(inv.getArgument(0));
                return builder;
            });
            when(builder.toUriString()).thenAnswer(inv -> "http://host" + capturedPath.get());

            doNothing().when(fileStorageService).uploadInputStream(anyString(), any(InputStream.class), anyLong(), anyString(), eq(mediaBucketName));

            FileUploadResponse response = fileUploadService.uploadFile(imageFile);

            assertNotNull(response);
            assertTrue(response.downloadUrl().endsWith(".jpg"));
            assertTrue(capturedPath.get().endsWith(".jpg"));

            verify(fileStorageService, times(1)).uploadInputStream(
                    anyString(), any(InputStream.class), eq(imageFile.getSize()), eq(imageFile.getContentType()), eq(mediaBucketName));
        }
    }

    // ----------------------- ВАЛИДАЦИИ uploadFile -----------------------
    @Test
    @DisplayName("uploadFile — успех: parsable-документ (.txt) уходит в mediaBucket")
    void uploadFile_ShouldUploadParsableDocumentToMediaBucket() {
        MultipartFile textFile = new MockMultipartFile(
                "document.txt", "document.txt", "text/plain", "text data".getBytes()
        );
        doNothing().when(fileStorageService).uploadInputStream(anyString(), any(InputStream.class), anyLong(), anyString(), eq(mediaBucketName));

        try (MockedStatic<ServletUriComponentsBuilder> mocked = mockStatic(ServletUriComponentsBuilder.class)) {
            ServletUriComponentsBuilder builder = mock(ServletUriComponentsBuilder.class, RETURNS_SELF);
            mocked.when(ServletUriComponentsBuilder::fromCurrentContextPath).thenReturn(builder);
            when(builder.path(anyString())).thenReturn(builder);
            when(builder.toUriString()).thenReturn("http://some-url.com/file.txt");

            fileUploadService.uploadFile(textFile);
        }

        verify(fileStorageService, times(1)).uploadInputStream(
                anyString(), any(InputStream.class), eq(textFile.getSize()), eq(textFile.getContentType()), eq(mediaBucketName));
    }


    @Test
    @DisplayName("uploadFile — успех: .webm конвертируется в .mp3 и уходит в voiceBucket")
    void uploadFile_ShouldConvertWebmToMp3AndUploadToVoiceBucket() {
        MultipartFile webmFile = new MockMultipartFile(
                "audio.webm", "audio.webm", "video/webm", "webm data".getBytes()
        );

        try (MockedConstruction<Encoder> mockedEncoder = mockConstruction(Encoder.class, (mock, context) -> {
            doAnswer(invocation -> {
                File targetFile = invocation.getArgument(1);
                Files.write(targetFile.toPath(), "mp3 data".getBytes());
                return null;
            }).when(mock).encode(any(MultimediaObject.class), any(File.class), any(EncodingAttributes.class));
        })) {

            AtomicReference<String> capturedPath = new AtomicReference<>("");
            try (MockedStatic<ServletUriComponentsBuilder> mockedUriBuilder = mockStatic(ServletUriComponentsBuilder.class)) {
                ServletUriComponentsBuilder builder = mock(ServletUriComponentsBuilder.class, RETURNS_SELF);
                mockedUriBuilder.when(ServletUriComponentsBuilder::fromCurrentContextPath).thenReturn(builder);
                when(builder.path(anyString())).thenAnswer(inv -> {
                    capturedPath.set(inv.getArgument(0));
                    return builder;
                });
                when(builder.toUriString()).thenAnswer(inv -> "http://host" + capturedPath.get());
                doNothing().when(fileStorageService).uploadInputStream(anyString(), any(InputStream.class), anyLong(), anyString(), eq(voiceBucketName));

                FileUploadResponse response = fileUploadService.uploadFile(webmFile);

                assertNotNull(response);
                assertTrue(response.message().contains("converted to MP3 and uploaded successfully"));
                assertTrue(response.downloadUrl().endsWith(".mp3"));
                assertTrue(capturedPath.get().endsWith(".mp3"));
                assertEquals("audio.webm", response.originalFilename());
                assertEquals("audio/mpeg", response.contentType());
                assertEquals("mp3 data".length(), response.size());
                assertEquals(1, mockedEncoder.constructed().size());

                ArgumentCaptor<String> objectKeyCaptor = ArgumentCaptor.forClass(String.class);
                verify(fileStorageService, times(1)).uploadInputStream(
                        objectKeyCaptor.capture(), any(InputStream.class), eq((long) "mp3 data".length()), eq("audio/mpeg"), eq(voiceBucketName));
                assertTrue(objectKeyCaptor.getValue().endsWith(".mp3"));
            }
        }
    }

    // ----------------------- ВАЛИДАЦИИ uploadFile -----------------------

    @Test
    @DisplayName("uploadFile — пустое имя пользователя → BadRequestException")
    void uploadFile_WhenUsernameIsEmpty_ShouldThrowBadRequestException() {
        mockAuthentication("");
        MultipartFile file = new MockMultipartFile("test.txt", "data".getBytes());

        BadRequestException exception = assertThrows(BadRequestException.class, () -> fileUploadService.uploadFile(file));

        assertEquals("Username from security context is empty or null.", exception.getMessage());
        verify(fileStorageService, never()).uploadInputStream(any(), any(), anyLong(), any(), any());
    }

    @Test
    @DisplayName("uploadFile — пустой файл → BadRequestException")
    void uploadFile_WhenFileIsEmpty_ShouldThrowBadRequestException() {
        MultipartFile emptyFile = new MockMultipartFile("empty.mp3", new byte[0]);

        BadRequestException exception = assertThrows(BadRequestException.class, () -> fileUploadService.uploadFile(emptyFile));

        assertEquals("File provided for upload is empty.", exception.getMessage());
        verify(fileStorageService, never()).uploadInputStream(any(), any(), anyLong(), any(), any());
    }

    @Test
    @DisplayName("uploadFile — неподдерживаемое расширение → BadRequestException")
    void uploadFile_WhenFileHasUnsupportedExtension_ShouldThrowBadRequestException() {
        MultipartFile unsupportedFile = new MockMultipartFile(
                "doc.docx", "doc.docx", "application/doc", "doc data".getBytes()
        );
        BadRequestException exception = assertThrows(BadRequestException.class, () -> fileUploadService.uploadFile(unsupportedFile));
        assertEquals("File extension is not supported or cannot identify file type after processing", exception.getMessage());
        verify(fileStorageService, never()).uploadInputStream(any(), any(), anyLong(), any(), any());
    }

    @Test
    @DisplayName("uploadFile — IOException при чтении потока → BadRequestException")
    void uploadFile_WhenIOExceptionDuringInputStreamAccess_ShouldThrowBadRequestException() throws IOException {
        MultipartFile audioFile = mock(MultipartFile.class);
        when(audioFile.getOriginalFilename()).thenReturn("audio.mp3");
        when(audioFile.getContentType()).thenReturn("audio/mpeg");
        when(audioFile.getSize()).thenReturn(100L);
        when(audioFile.isEmpty()).thenReturn(false);
        when(audioFile.getInputStream()).thenThrow(new IOException("Simulated IO error"));

        BadRequestException exception = assertThrows(BadRequestException.class, () -> fileUploadService.uploadFile(audioFile));

        assertTrue(exception.getMessage().contains("Error processing uploaded file"));
        assertInstanceOf(IOException.class, exception.getCause());
        verify(fileStorageService, never()).uploadInputStream(any(), any(), anyLong(), any(), any());
    }

    @Test
    @DisplayName("uploadFile — ошибка конвертации .webm → BadRequestException")
    void uploadFile_WhenConversionFails_ShouldThrowBadRequestException() {
        MultipartFile webmFile = new MockMultipartFile(
                "audio.webm", "audio.webm", "video/webm", "webm data".getBytes()
        );

        try (MockedConstruction<Encoder> mockedEncoder = mockConstruction(Encoder.class, (mock, context) -> {
            doThrow(EncoderException.class)
                    .when(mock).encode(any(MultimediaObject.class), any(File.class), any(EncodingAttributes.class));
        })) {

            BadRequestException exception = assertThrows(BadRequestException.class, () -> fileUploadService.uploadFile(webmFile));

            assertTrue(exception.getMessage().contains("Error processing uploaded file"));
            assertInstanceOf(IOException.class, exception.getCause());
            assertEquals(1, mockedEncoder.constructed().size());
            verify(fileStorageService, never()).uploadInputStream(any(), any(), anyLong(), any(), any());
        }
    }

    @Test
    @DisplayName("uploadFile — исключение хранилища пробрасывается как есть")
    void uploadFile_WhenStorageExceptionOccurs_ShouldPropagateStorageException() {
        MultipartFile audioFile = new MockMultipartFile(
                "audio.mp3", "audio.mp3", "audio/mpeg", "audio data".getBytes()
        );

        // uploadInputStream бросает исключение → метод завершится раньше, чем начнётся сборка URL
        doThrow(new StorageException("Simulated storage error"))
                .when(fileStorageService).uploadInputStream(anyString(), any(InputStream.class), anyLong(), anyString(), eq(voiceBucketName));

        StorageException exception = assertThrows(StorageException.class, () -> fileUploadService.uploadFile(audioFile));
        assertEquals("Simulated storage error", exception.getMessage());
    }

    // ----------------------- downloadFile -----------------------

    @Test
    @DisplayName("downloadFile — успешно: аудио → voiceBucket")
    void downloadFile_ShouldDownloadAudioSuccessfully() {
        String objectKey = "12345_uuid.mp3";
        DownloadableFile mockDownloadableFile = mock(DownloadableFile.class);
        when(fileStorageService.downloadFile(objectKey, voiceBucketName)).thenReturn(mockDownloadableFile);

        DownloadableFile result = fileUploadService.downloadFile(objectKey);

        assertNotNull(result);
        assertEquals(mockDownloadableFile, result);
        verify(fileStorageService, times(1)).downloadFile(objectKey, voiceBucketName);
    }

    @Test
    @DisplayName("downloadFile — успешно: медиа → mediaBucket")
    void downloadFile_ShouldDownloadMediaSuccessfully() {
        String objectKey = "12345_uuid.mp4";
        DownloadableFile mockDownloadableFile = mock(DownloadableFile.class);
        when(fileStorageService.downloadFile(objectKey, mediaBucketName)).thenReturn(mockDownloadableFile);

        DownloadableFile result = fileUploadService.downloadFile(objectKey);

        assertNotNull(result);
        assertEquals(mockDownloadableFile, result);
        verify(fileStorageService, times(1)).downloadFile(objectKey, mediaBucketName);
    }

    @Test
    @DisplayName("downloadFile — успешно: parsable-документ (.txt) → mediaBucket")
    void downloadFile_ShouldDownloadParsableDocumentSuccessfully() {
        String objectKey = "12345_uuid.txt";
        DownloadableFile mockDownloadableFile = mock(DownloadableFile.class);
        when(fileStorageService.downloadFile(objectKey, mediaBucketName)).thenReturn(mockDownloadableFile);

        DownloadableFile result = fileUploadService.downloadFile(objectKey);

        assertNotNull(result);
        assertEquals(mockDownloadableFile, result);
        verify(fileStorageService, times(1)).downloadFile(objectKey, mediaBucketName);
    }

    @Test
    @DisplayName("downloadFile — null objectKey → BadRequestException")
    void downloadFile_WhenObjectKeyIsNull_ShouldThrowBadRequestException() {
        BadRequestException exception = assertThrows(BadRequestException.class, () -> fileUploadService.downloadFile(null));
        assertEquals("Object key for download cannot be null or empty.", exception.getMessage());
        verify(fileStorageService, never()).downloadFile(anyString(), anyString());
    }

    @Test
    @DisplayName("downloadFile — неподдерживаемое расширение → BadRequestException")
    void downloadFile_WhenExtensionIsUnsupported_ShouldThrowBadRequestException() {
        String objectKey = "123_uuid.docx";
        BadRequestException exception = assertThrows(BadRequestException.class, () -> fileUploadService.downloadFile(objectKey));
        assertEquals("File extension is not supported or cannot identify file type", exception.getMessage());
        verify(fileStorageService, never()).downloadFile(anyString(), anyString());
    }

    @Test
    @DisplayName("downloadFile — ResourceNotFound пробрасывается")
    void downloadFile_WhenResourceNotFoundExceptionOccurs_ShouldPropagateResourceNotFoundException() {
        String objectKey = "nonexistent.mp3";
        when(fileStorageService.downloadFile(objectKey, voiceBucketName)).thenThrow(new ResourceNotFoundException("File not found"));

        ResourceNotFoundException exception = assertThrows(ResourceNotFoundException.class, () -> fileUploadService.downloadFile(objectKey));

        assertEquals("File not found", exception.getMessage());
    }
}
