package software.pxel.learneasy.service;

import org.springframework.data.domain.Pageable;
import software.pxel.learneasy.api.dto.article.ArticleInfoResponse;
import software.pxel.learneasy.api.dto.article.ArticleResponse;
import software.pxel.learneasy.api.dto.article.CreateArticleRequest;
import software.pxel.learneasy.api.dto.article.UpdateArticleRequest;
import software.pxel.learneasy.api.dto.pageable.PageableResponse;

public interface ArticleService {
    ArticleResponse createArticle(CreateArticleRequest request);

    ArticleResponse updateArticle(Long articleId, UpdateArticleRequest request);

    void deleteArticle(Long articleId);

    ArticleResponse getActiveBySlug(String slug);

    PageableResponse<ArticleInfoResponse> getActiveArticlesInfo(Pageable pageable);

    PageableResponse<ArticleInfoResponse> getAllArticlesInfo(Pageable pageable);
}
