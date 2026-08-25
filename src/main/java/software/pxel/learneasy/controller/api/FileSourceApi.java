package software.pxel.learneasy.controller.api;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.core.io.Resource;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.MultipartFile;
import software.pxel.learneasy.api.dto.common.ErrorResponse;
import software.pxel.learneasy.api.dto.s3.FileUploadResponse;

@Tag(name = "File Source API", description = "API для загрузки и скачивания файлов через S3-совместимое хранилище")
public interface FileSourceApi {

    @Operation(
            summary = "Загрузить файл",
            description = "Позволяет аутентифицированному пользователю (с ролью 'USER' или 'ADMIN') загрузить файл. " +
                    "Файл сохраняется в S3-хранилище, возвращается информация о сохраненном объекте.",
            responses = {
                    @ApiResponse(responseCode = "200", description = "Файл успешно загружен",
                            content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                                    schema = @Schema(implementation = FileUploadResponse.class))),
                    @ApiResponse(responseCode = "400", description = "Некорректный запрос (например, пустой файл, неверный тип файла)",
                            content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                                    schema = @Schema(implementation = ErrorResponse.class))),
                    @ApiResponse(responseCode = "401", description = "Неавторизованный доступ",
                            content = @Content),
                    @ApiResponse(responseCode = "403", description = "Доступ запрещен (недостаточно прав)",
                            content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                                    schema = @Schema(implementation = ErrorResponse.class))),
                    @ApiResponse(responseCode = "413", description = "Размер загружаемого файла слишком большой",
                            content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                                    schema = @Schema(implementation = ErrorResponse.class))),
                    @ApiResponse(responseCode = "500", description = "Внутренняя ошибка сервера (например, ошибка при работе с хранилищем)",
                            content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                                    schema = @Schema(implementation = ErrorResponse.class)))
            }
    )
    @SecurityRequirement(name = "bearerAuth")
    ResponseEntity<FileUploadResponse> uploadFile(
            @Parameter(description = "Файл для загрузки (например, .mp3, .jpg, .mp4)", required = true,
                    content = @Content(mediaType = MediaType.MULTIPART_FORM_DATA_VALUE))
            @RequestParam("file") MultipartFile file
    );

    @Operation(
            summary = "Скачать файл",
            description = "Позволяет всем скачать файл по его ключу объекта из S3-хранилища. " +
                    "Файл возвращается как поток байт (octet-stream).",
            responses = {
                    @ApiResponse(responseCode = "200", description = "Файл успешно получен для скачивания",
                            content = @Content(mediaType = MediaType.APPLICATION_OCTET_STREAM_VALUE,
                                    schema = @Schema(type = "string", format = "binary"))),
                    @ApiResponse(responseCode = "400", description = "Некорректный запрос (например, пустой objectKey)",
                            content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                                    schema = @Schema(implementation = ErrorResponse.class))),
                    @ApiResponse(responseCode = "401", description = "Неавторизованный доступ",
                            content = @Content),
                    @ApiResponse(responseCode = "403", description = "Доступ запрещен (недостаточно прав)",
                            content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                                    schema = @Schema(implementation = ErrorResponse.class))),
                    @ApiResponse(responseCode = "404", description = "Файл не найден в хранилище",
                            content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                                    schema = @Schema(implementation = ErrorResponse.class))),
                    @ApiResponse(responseCode = "500", description = "Внутренняя ошибка сервера (например, ошибка при работе с хранилищем)",
                            content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                                    schema = @Schema(implementation = ErrorResponse.class)))
            }
    )
    ResponseEntity<Resource> downloadFile(
            @Parameter(description = "Ключ объекта (путь к файлу в S3-хранилище)", required = true, example = "1748616789621_fe265923-658a-45d2-8ff5-d2285bc94c0c.mp3")
            @PathVariable String objectKey
    );
}
