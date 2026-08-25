package software.pxel.learneasy.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import software.pxel.learneasy.controller.api.FileSourceApi;
import software.pxel.learneasy.api.dto.s3.DownloadableFile;
import software.pxel.learneasy.api.dto.s3.FileUploadResponse;
import software.pxel.learneasy.service.FileUploadService;

import static software.pxel.learneasy.constants.ApiRoutes.FILE_STORAGE_DOWNLOAD_ROUTE;
import static software.pxel.learneasy.constants.ApiRoutes.FILE_STORAGE_URI;

@RequiredArgsConstructor
@RestController
@RequestMapping(FILE_STORAGE_URI)
public class FileUploadController implements FileSourceApi {

    private final FileUploadService fileUploadService;

    @Override
    @PostMapping(value = "/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("hasAnyAuthority('USER', 'ADMIN')")
    public ResponseEntity<FileUploadResponse> uploadFile(@RequestParam("file") MultipartFile file) {
        FileUploadResponse response = fileUploadService.uploadFile(file);
        return ResponseEntity.ok(response);
    }

    @Override
    @GetMapping(FILE_STORAGE_DOWNLOAD_ROUTE + "/{objectKey}")
    public ResponseEntity<Resource> downloadFile(@PathVariable String objectKey) {
        DownloadableFile downloadableFile = fileUploadService.downloadFile(objectKey);

        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(downloadableFile.contentType()))
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + downloadableFile.filename() + "\"")
                .contentLength(downloadableFile.contentLength())
                .body(downloadableFile.resource());
    }
}
