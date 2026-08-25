package software.pxel.learneasy.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import software.pxel.learneasy.api.dto.article.ArticleInfoResponse;
import software.pxel.learneasy.api.dto.article.ArticleResponse;
import software.pxel.learneasy.api.dto.article.CreateArticleRequest;
import software.pxel.learneasy.api.dto.article.UpdateArticleRequest;
import software.pxel.learneasy.api.dto.pageable.PageableResponse;
import software.pxel.learneasy.controller.api.ArticleApi;
import software.pxel.learneasy.service.ArticleService;

import static software.pxel.learneasy.constants.ApiRoutes.ARTICLES_URI;

@RequiredArgsConstructor
@RestController
@RequestMapping(ARTICLES_URI)
@Slf4j
public class ArticleController implements ArticleApi {

    private final ArticleService articleService;

    @Override
    @PostMapping
    @PreAuthorize("hasAuthority('ADMIN')")
    public ResponseEntity<ArticleResponse> createArticle(@Valid @RequestBody CreateArticleRequest request) {
        log.info("POST request to create an article");
        ArticleResponse response = articleService.createArticle(request);
        return new ResponseEntity<>(response, HttpStatus.CREATED);
    }

    @Override
    @PutMapping("/{articleId}")
    @PreAuthorize("hasAuthority('ADMIN')")
    public ResponseEntity<ArticleResponse> updateArticle(
            @PathVariable Long articleId,
            @Valid @RequestBody UpdateArticleRequest request) {
        log.info("PUT request to update article {}", articleId);
        ArticleResponse response = articleService.updateArticle(articleId, request);
        return ResponseEntity.ok(response);
    }

    @Override
    @DeleteMapping("/{articleId}")
    @PreAuthorize("hasAuthority('ADMIN')")
    public ResponseEntity<Void> deleteArticle(@PathVariable Long articleId) {
        log.info("DELETE request to delete article {}", articleId);
        articleService.deleteArticle(articleId);
        return ResponseEntity.noContent().build();
    }

    @Override
    @GetMapping("/{slug}")
    public ResponseEntity<ArticleResponse> getArticleBySlug(@PathVariable String slug) {
        ArticleResponse response = articleService.getActiveBySlug(slug);
        return ResponseEntity.ok(response);
    }

    @Override
    @GetMapping("/info")
    public ResponseEntity<PageableResponse<ArticleInfoResponse>> getActiveArticlesInfo(int page, int size) {
        PageableResponse<ArticleInfoResponse> response = articleService.getActiveArticlesInfo(PageRequest.of(page - 1, size));
        return ResponseEntity.ok(response);
    }

    @Override
    @GetMapping("/all/info")
    @PreAuthorize("hasAuthority('ADMIN')")
    public ResponseEntity<PageableResponse<ArticleInfoResponse>> getAllArticlesInfo(int page, int size) {
        PageableResponse<ArticleInfoResponse> response = articleService.getAllArticlesInfo(PageRequest.of(page - 1, size));
        return ResponseEntity.ok(response);
    }
}
