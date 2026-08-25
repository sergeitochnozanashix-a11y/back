package software.pxel.learneasy.service.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.cache.annotation.Caching;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import software.pxel.learneasy.api.dto.article.ArticleInfoResponse;
import software.pxel.learneasy.api.dto.article.ArticleResponse;
import software.pxel.learneasy.api.dto.article.CreateArticleRequest;
import software.pxel.learneasy.api.dto.article.UpdateArticleRequest;
import software.pxel.learneasy.api.dto.pageable.PageableResponse;
import software.pxel.learneasy.exception.ResourceNotFoundException;
import software.pxel.learneasy.mapper.ArticleMapper;
import software.pxel.learneasy.model.Article;
import software.pxel.learneasy.repository.ArticleRepository;
import software.pxel.learneasy.service.ArticleService;
import software.pxel.learneasy.util.SlugGenerator;
import software.pxel.learneasy.util.pageable.PageableResponseUtil;

@Service
@Slf4j
@RequiredArgsConstructor
public class ArticleServiceImpl implements ArticleService {

    private static final String ARTICLES_CACHE = "articles";
    private static final String ARTICLE_BY_SLUG_CACHE = "articleBySlug";

    private final ArticleMapper articleMapper;
    private final ArticleRepository articleRepository;
    private final PageableResponseUtil pageableResponseUtil;

    @Override
    @Transactional
    @CacheEvict(value = ARTICLES_CACHE, allEntries = true)
    public ArticleResponse createArticle(CreateArticleRequest request) {
        log.info("Creating a new article with title: '{}'", request.title());

        Article article = articleMapper.fromCreateRequest(request);
        article.setSlug(generateUniqueSlug(request.title()));

        Article savedArticle = articleRepository.save(article);
        log.info("Article created with ID: {} and slug: {}", savedArticle.getId(), savedArticle.getSlug());

        return articleMapper.toResponse(savedArticle);
    }

    @Override
    @Transactional
    @Caching(evict = {
            @CacheEvict(value = ARTICLES_CACHE, allEntries = true),
            @CacheEvict(value = ARTICLE_BY_SLUG_CACHE, key = "#result.slug")
    })
    public ArticleResponse updateArticle(Long articleId, UpdateArticleRequest request) {
        log.info("Updating article with ID: {}", articleId);
        Article article = articleRepository.findById(articleId)
                .orElseThrow(() -> new ResourceNotFoundException("Article not found with id: " + articleId));

        articleMapper.updateFromRequest(request, article);

        if (request.slug() != null && !request.slug().isBlank()) {
            article.setSlug(generateUniqueSlug(request.slug(), articleId));
        } else {
            article.setSlug(generateUniqueSlug(request.title(), articleId));
        }

        Article updatedArticle = articleRepository.save(article);
        log.info("Article with ID: {} was updated", updatedArticle.getId());

        return articleMapper.toResponse(updatedArticle);
    }

    @Override
    @Transactional
    @Caching(evict = {
            @CacheEvict(value = ARTICLES_CACHE, allEntries = true),
            @CacheEvict(value = ARTICLE_BY_SLUG_CACHE, allEntries = true)
    })
    public void deleteArticle(Long articleId) {
        log.info("Deleting article with ID: {}", articleId);
        if (!articleRepository.existsById(articleId)) {
            throw new ResourceNotFoundException("Article not found with id: " + articleId);
        }
        articleRepository.deleteById(articleId);
        log.info("Article with ID: {} was deleted", articleId);
    }

    @Override
    @Transactional(readOnly = true)
    @Cacheable(value = ARTICLE_BY_SLUG_CACHE, key = "#slug")
    public ArticleResponse getActiveBySlug(String slug) {
        return articleRepository.findBySlugAndActive(slug, true)
                .map(articleMapper::toResponse)
                .orElseThrow(() -> new ResourceNotFoundException("Active article not found with slug: " + slug));
    }

    @Override
    @Transactional(readOnly = true)
    @Cacheable(value = ARTICLES_CACHE)
    public PageableResponse<ArticleInfoResponse> getActiveArticlesInfo(Pageable pageable) {
        Page<ArticleInfoResponse> rowArticles = articleRepository.findProjectionByActive(true, pageable)
                .map(articleMapper::toInfoResponse);

        return pageableResponseUtil.buildPageableResponse(rowArticles.getContent(), rowArticles, new PageableResponse<>());
    }

    @Override
    @Transactional(readOnly = true)
    @Cacheable(value = ARTICLES_CACHE)
    public PageableResponse<ArticleInfoResponse> getAllArticlesInfo(Pageable pageable) {
        Page<ArticleInfoResponse> rowArticles = articleRepository.findAllProjectionBy(pageable)
                .map(articleMapper::toInfoResponse);

        return pageableResponseUtil.buildPageableResponse(rowArticles.getContent(), rowArticles, new PageableResponse<>());
    }

    private String generateUniqueSlug(String source) {
        return generateUniqueSlug(source, null);
    }

    private String generateUniqueSlug(String source, Long currentId) {
        String slug = SlugGenerator.toSlug(source);
        return articleRepository.findBySlug(slug)
                .filter(article -> !article.getId().equals(currentId))
                .map(existing -> slug + "-" + System.currentTimeMillis())
                .orElse(slug);
    }
}
