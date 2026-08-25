package software.pxel.learneasy.service.impl;

import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;
import software.pxel.learneasy.api.dto.s3.DownloadableFile;
import software.pxel.learneasy.api.dto.s3.FileUploadResponse;
import software.pxel.learneasy.exception.BadRequestException;
import software.pxel.learneasy.exception.StorageException;
import software.pxel.learneasy.model.enums.FileType;
import software.pxel.learneasy.service.FileStorageService;
import software.pxel.learneasy.service.FileSupportService;
import software.pxel.learneasy.service.FileUploadService;
import ws.schild.jave.Encoder;
import ws.schild.jave.EncoderException;
import ws.schild.jave.MultimediaObject;
import ws.schild.jave.encode.AudioAttributes;
import ws.schild.jave.encode.EncodingAttributes;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import static software.pxel.learneasy.constants.ApiRoutes.FILE_STORAGE_DOWNLOAD_ROUTE;
import static software.pxel.learneasy.constants.ApiRoutes.FILE_STORAGE_URI;

@Slf4j
@Service
public class FileUploadServiceImpl implements FileUploadService {

    private static final Map<String, String> CONTENT_TYPE_TO_EXTENSION = Map.ofEntries(
            Map.entry("audio/mpeg", ".mp3"),
            Map.entry("audio/wav", ".wav"),
            Map.entry("audio/x-wav", ".wav"),
            Map.entry("audio/ogg", ".ogg"),
            Map.entry("audio/mp4", ".m4a"),
            Map.entry("audio/x-m4a", ".m4a"),
            Map.entry("audio/aac", ".aac"),
            Map.entry("audio/webm", ".webm"),
            Map.entry("video/mp4", ".mp4"),
            Map.entry("video/quicktime", ".mov"),
            Map.entry("video/x-msvideo", ".avi"),
            Map.entry("video/x-matroska", ".mkv"),
            Map.entry("video/webm", ".webm"),
            Map.entry("image/jpeg", ".jpg"),
            Map.entry("image/png", ".png"),
            Map.entry("image/gif", ".gif"),
            Map.entry("image/webp", ".webp"),
            Map.entry("image/bmp", ".bmp"),
            Map.entry("text/plain", ".txt"),
            Map.entry("text/csv", ".csv"),
            Map.entry("text/markdown", ".md")
    );

    private static final Set<String> STATICALLY_SUPPORTED_MEDIA_EXTENSIONS = Set.of(
            ".mp3", ".wav", ".ogg", ".m4a", ".aac",
            ".mp4", ".mov", ".avi", ".mkv", ".webm",
            ".jpg", ".jpeg", ".png", ".gif", ".webp", ".bmp"
    );

    private static final String UNDETERMINED_EXTENSION_MSG =
            "Could not determine a standard extension for file '{}', content type '{}'. Using empty extension.";

    private final FileStorageService fileStorageService;
    private final FileSupportService fileSupportService;
    private final String voiceBucketName;
    private final String mediaBucketName;

    private Set<String> allSupportedExtensions;
    private Set<String> parsableFileExtensions;


    public FileUploadServiceImpl(FileStorageService fileStorageService,
                                 FileSupportService fileSupportService,
                                 @Qualifier("minioVoiceBucketName") String voiceBucketName,
                                 @Qualifier("minioMediaBucketName") String mediaBucketName) {
        this.fileStorageService = fileStorageService;
        this.fileSupportService = fileSupportService;
        this.voiceBucketName = voiceBucketName;
        this.mediaBucketName = mediaBucketName;
    }

    @PostConstruct
    private void initializeSupportedExtensions() {
        this.parsableFileExtensions = this.fileSupportService.getParsableFileExtensions();
        Set<String> parsableExtensionsWithDot = this.parsableFileExtensions.stream()
                .map(ext -> "." + ext)
                .collect(Collectors.toSet());

        this.allSupportedExtensions = new HashSet<>();
        this.allSupportedExtensions.addAll(STATICALLY_SUPPORTED_MEDIA_EXTENSIONS);
        this.allSupportedExtensions.addAll(parsableExtensionsWithDot);
        log.info("Initialized FileUploadService with {} total supported extensions.", allSupportedExtensions.size());
    }


    @Override
    public FileUploadResponse uploadFile(MultipartFile file) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String username = authentication.getName();

        if (username == null || username.trim().isEmpty()) {
            throw new BadRequestException("Username from security context is empty or null.");
        }

        if (file.isEmpty()) {
            log.warn("Upload attempt with empty file by user: {}", username);
            throw new BadRequestException("File provided for upload is empty.");
        }

        String originalFilename = file.getOriginalFilename();
        String incomingContentType = file.getContentType();
        String detectedExtension = determineFileExtension(originalFilename, incomingContentType);

        String finalExtension = detectedExtension;
        String finalContentType = incomingContentType;
        long finalFileSize = file.getSize();
        InputStream inputStreamToUpload = null;

        Path tempOriginalPath = null;
        Path tempConvertedPath = null;
        boolean converted = false;

        try {
            boolean isWebm = ".webm".equals(detectedExtension)
                    || "video/webm".equalsIgnoreCase(incomingContentType)
                    || "audio/webm".equalsIgnoreCase(incomingContentType);

            if (isWebm) {
                log.info("Detected WEBM file form user '{}'. Starting conversion to MP3.", username);
                tempOriginalPath = Files.createTempFile("upload_" + UUID.randomUUID(), ".webm");
                file.transferTo(tempOriginalPath.toFile());

                tempConvertedPath = Files.createTempFile("converted_" + UUID.randomUUID(), ".mp3");
                convertWebmToMp3(tempOriginalPath.toFile(), tempConvertedPath.toFile());

                File convertedFile = tempConvertedPath.toFile();
                finalExtension = ".mp3";
                finalContentType = "audio/mpeg";
                finalFileSize = convertedFile.length();
                inputStreamToUpload = new FileInputStream(convertedFile);
                converted = true;
                log.info("Conversion successful. New size: {}", finalFileSize);
            } else {
                inputStreamToUpload = file.getInputStream();
            }

            log.info("Processing file for upload: originalName='{}', finalType='{}', size={}, user='{}', converted={}",
                    originalFilename, finalContentType, finalFileSize, username, converted);

            FileType fileType = determineFileType(finalExtension);
            if (fileType.equals(FileType.UNKNOWN) || finalExtension.isEmpty()) {
                throw new BadRequestException("File extension is not supported or cannot identify file type after processing");
            }

            String bucketName = switch (fileType) {
                case AUDIO -> voiceBucketName;
                case MEDIA, PARSABLE_DOCUMENT -> mediaBucketName;
                default -> throw new IllegalStateException("Unexpected file type: " + fileType);
            };

            String objectKey = System.currentTimeMillis() + "_" + UUID.randomUUID() + finalExtension;

            fileStorageService.uploadInputStream(objectKey, inputStreamToUpload, finalFileSize, finalContentType, bucketName);
            log.info("File uploaded successfully for user '{}'. ObjectKey: {}", username, objectKey);

            String downloadUrl = ServletUriComponentsBuilder.fromCurrentContextPath()
                    .path(FILE_STORAGE_URI + FILE_STORAGE_DOWNLOAD_ROUTE + "/" + objectKey)
                    .toUriString();

            return new FileUploadResponse(
                    converted ? "File converted to MP3 and uploaded successfully!" : "File uploaded successfully!",
                    downloadUrl,
                    originalFilename,
                    finalContentType,
                    finalFileSize
            );

        } catch (IOException e) {
            log.error("IOException while processing file for user '{}': {}", username, e.getMessage(), e);
            throw new BadRequestException("Error processing uploaded file: " + e.getMessage(), e);
        } catch (StorageException e) {
            log.error("Storage error during file upload for user '{}': {}", username, e.getMessage(), e);
            throw e;
        } finally {
            if (inputStreamToUpload != null) {
                try {
                    inputStreamToUpload.close();
                } catch (IOException e) {
                    log.warn("Could not close input stream", e);
                }
            }
            deleteTempFile(tempOriginalPath);
            deleteTempFile(tempConvertedPath);
        }
    }

    private void convertWebmToMp3(File source, File target) throws IOException {
        try {
            AudioAttributes audio = new AudioAttributes();
            audio.setCodec("libmp3lame");
            audio.setBitRate(128000);
            audio.setChannels(2);
            audio.setSamplingRate(44100);

            EncodingAttributes attrs = new EncodingAttributes();
            attrs.setOutputFormat("mp3");
            attrs.setAudioAttributes(audio);

            Encoder encoder = new Encoder();
            encoder.encode(new MultimediaObject(source), target, attrs);
        } catch (EncoderException e) {
            log.error("JAVE2 FFmpeg conversion failed", e);
            throw new IOException("FFmpeg conversion failed: " + e.getMessage(), e);
        }
    }

    private void deleteTempFile(Path path) {
        if (path != null) {
            try {
                Files.deleteIfExists(path);
                log.debug("Deleted temp file: {}", path);
            } catch (IOException e) {
                log.warn("Failed to delete temp file: {}", path, e);
            }
        }
    }

    private FileType determineFileType(String extension) {
        if (extension == null || extension.isEmpty()) {
            return FileType.UNKNOWN;
        }

        String extWithoutDot = extension.substring(1).toLowerCase();

        if (parsableFileExtensions.contains(extWithoutDot)) {
            return FileType.PARSABLE_DOCUMENT;
        }

        if (Set.of(".mp3", ".wav", ".ogg", ".m4a", ".aac").contains(extension)) {
            return FileType.AUDIO;
        }

        if (STATICALLY_SUPPORTED_MEDIA_EXTENSIONS.contains(extension)) {
            return FileType.MEDIA;
        }

        return FileType.UNKNOWN;
    }

    private String determineFileExtension(String originalFilename, String contentType) {
        if (StringUtils.hasText(originalFilename)) {
            int dotIndex = originalFilename.lastIndexOf(".");
            if (dotIndex != -1 && dotIndex < originalFilename.length() - 1) {
                String extension = originalFilename.substring(dotIndex).toLowerCase();
                if (allSupportedExtensions.contains(extension)) {
                    return extension;
                }
            }
        }

        if (StringUtils.hasText(contentType)) {
            String extension = CONTENT_TYPE_TO_EXTENSION.get(contentType.toLowerCase());
            if (extension != null) {
                return extension;
            }
        }

        log.warn(UNDETERMINED_EXTENSION_MSG, originalFilename, contentType);
        return "";
    }

    @Override
    public DownloadableFile downloadFile(String objectKey) {
        if (!StringUtils.hasText(objectKey)) {
            log.warn("Download attempt with empty or null objectKey.");
            throw new BadRequestException("Object key for download cannot be null or empty.");
        }
        log.info("Attempting to download file with objectKey: {}", objectKey);

        String fileExtension = objectKey.substring(objectKey.lastIndexOf(".")).toLowerCase();
        FileType fileType = determineFileType(fileExtension);

        if (fileType.equals(FileType.UNKNOWN)) {
            throw new BadRequestException("File extension is not supported or cannot identify file type");
        }

        String bucketName = switch (fileType) {
            case AUDIO -> voiceBucketName;
            case MEDIA, PARSABLE_DOCUMENT -> mediaBucketName;
            default -> throw new IllegalStateException("Unexpected file type: " + fileType);
        };

        return fileStorageService.downloadFile(objectKey, bucketName);
    }
}
