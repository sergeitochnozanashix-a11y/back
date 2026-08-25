package software.pxel.learneasy.controller.api;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import software.pxel.learneasy.api.dto.article.ArticleInfoResponse;
import software.pxel.learneasy.api.dto.article.ArticleResponse;
import software.pxel.learneasy.api.dto.article.CreateArticleRequest;
import software.pxel.learneasy.api.dto.article.PageArticleInfo;
import software.pxel.learneasy.api.dto.article.UpdateArticleRequest;
import software.pxel.learneasy.api.dto.common.ErrorResponse;
import software.pxel.learneasy.api.dto.pageable.PageableResponse;

@Tag(name = "Article API", description = "API для управления статьями лендинга")
public interface ArticleApi {

    @Operation(
            summary = "Создать новую статью",
            description = "Создает новую статью. Доступно только администраторам.",
            security = @SecurityRequirement(name = "bearerAuth"),
            responses = {
                    @ApiResponse(responseCode = "201", description = "Статья успешно создана",
                            content = @Content(schema = @Schema(implementation = ArticleResponse.class))),
                    @ApiResponse(responseCode = "400", description = "Неверные данные",
                            content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
                    @ApiResponse(responseCode = "403", description = "Доступ запрещен")
            }
    )
    @PostMapping(consumes = MediaType.APPLICATION_JSON_VALUE)
    ResponseEntity<ArticleResponse> createArticle(@Valid @RequestBody CreateArticleRequest request);

    @Operation(
            summary = "Обновить статью",
            description = "Обновляет существующую статью по ID. Доступно только администраторам.",
            security = @SecurityRequirement(name = "bearerAuth"),
            responses = {
                    @ApiResponse(responseCode = "200", description = "Статья успешно обновлена",
                            content = @Content(schema = @Schema(implementation = ArticleResponse.class))),
                    @ApiResponse(responseCode = "404", description = "Статья не найдена",
                            content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
            }
    )
    @PutMapping(path = "/{articleId}", consumes = MediaType.APPLICATION_JSON_VALUE)
    ResponseEntity<ArticleResponse> updateArticle(
            @Parameter(description = "ID статьи", example = "1") @PathVariable Long articleId,
            @Valid @RequestBody UpdateArticleRequest request
    );

    @Operation(
            summary = "Удалить статью",
            description = "Удаляет статью по ID. Доступно только администраторам.",
            security = @SecurityRequirement(name = "bearerAuth"),
            responses = {
                    @ApiResponse(responseCode = "204", description = "Статья успешно удалена"),
                    @ApiResponse(responseCode = "404", description = "Статья не найдена",
                            content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
            }
    )
    @DeleteMapping(path = "/{articleId}")
    ResponseEntity<Void> deleteArticle(
            @Parameter(description = "ID статьи", example = "1") @PathVariable Long articleId
    );

    @Operation(
            summary = "Получить активную статью по slug",
            description = "Возвращает одну активную (опубликованную) статью по её уникальному slug. Включает полный контент.",
            responses = {
                    @ApiResponse(responseCode = "200", description = "Успешный ответ",
                            content = @Content(schema = @Schema(implementation = ArticleResponse.class))),
                    @ApiResponse(responseCode = "404", description = "Статья не найдена",
                            content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
            }
    )
    @GetMapping(path = "/{slug}")
    ResponseEntity<ArticleResponse> getArticleBySlug(
            @Parameter(description = "Slug статьи", example = "chto-takoe-solid") @PathVariable String slug
    );

    @Operation(
            summary = "Получить краткий список активных статей (без контента)",
            description = "Возвращает пагинированный список активных (опубликованных) статей без поля `content` для оптимизации.",
            responses = @ApiResponse(responseCode = "200", description = "Успешный ответ",
                    content = @Content(schema = @Schema(implementation = PageArticleInfo.class)))
    )
    @GetMapping("/info")
    ResponseEntity<PageableResponse<ArticleInfoResponse>> getActiveArticlesInfo(
            @RequestParam(name = "page", defaultValue = "1") int page,
            @RequestParam(name = "size", defaultValue = "6") int size
    );

    @Operation(
            summary = "Получить краткий список всех статей (для админки, без контента)",
            description = "Возвращает пагинированный список всех статей без поля `content` для оптимизации.",
            security = @SecurityRequirement(name = "bearerAuth"),
            responses = @ApiResponse(responseCode = "200", description = "Успешный ответ",
                    content = @Content(schema = @Schema(implementation = PageArticleInfo.class)))
    )
    @GetMapping("/all/info")
    ResponseEntity<PageableResponse<ArticleInfoResponse>> getAllArticlesInfo(
            @RequestParam(name = "page", defaultValue = "1") int page,
            @RequestParam(name = "size", defaultValue = "6") int size
    );
}
