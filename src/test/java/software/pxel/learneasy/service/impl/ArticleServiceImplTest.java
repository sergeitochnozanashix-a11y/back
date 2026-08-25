package software.pxel.learneasy.service.impl;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import software.pxel.learneasy.api.dto.article.ArticleInfoResponse;
import software.pxel.learneasy.api.dto.article.ArticleResponse;
import software.pxel.learneasy.api.dto.article.CreateArticleRequest;
import software.pxel.learneasy.api.dto.article.UpdateArticleRequest;
import software.pxel.learneasy.api.dto.pageable.PageableResponse;
import software.pxel.learneasy.exception.ResourceNotFoundException;
import software.pxel.learneasy.mapper.ArticleMapper;
import software.pxel.learneasy.model.Article;
import software.pxel.learneasy.repository.ArticleRepository;
import software.pxel.learneasy.repository.projection.ArticleInfoProjection;
import software.pxel.learneasy.util.pageable.PageableResponseUtil;
import software.pxel.learneasy.utilgenerator.PageableGenerator;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("ArticleService (с проекциями)")
class ArticleServiceImplTest {

    @Mock
    private ArticleRepository articleRepository;

    @Mock
    private ArticleMapper articleMapper;

    @Mock
    private PageableResponseUtil pageableResponseUtil;

    @InjectMocks
    private ArticleServiceImpl articleService;

    @Nested
    @DisplayName("createArticle")
    class CreateArticle {
        @Test
        @DisplayName("успешно создает статью")
        void shouldCreateArticle() {
            // Given
            CreateArticleRequest request = new CreateArticleRequest("Новый Заголовок", "Summary", "Content", false);
            Article articleToSave = new Article();
            when(articleMapper.fromCreateRequest(request)).thenReturn(articleToSave);
            when(articleRepository.findBySlug("novyy-zagolovok")).thenReturn(Optional.empty());
            when(articleRepository.save(any(Article.class))).thenAnswer(inv -> {
                Article a = inv.getArgument(0);
                a.setId(10L);
                return a;
            });
            ArticleResponse expectedResponse = new ArticleResponse(10L, "Новый Заголовок", "novyy-zagolovok", "Summary", "Content", false, Instant.now(), Instant.now());
            when(articleMapper.toResponse(any(Article.class))).thenReturn(expectedResponse);

            // When
            ArticleResponse response = articleService.createArticle(request);

            // Then
            assertNotNull(response);
            assertEquals(expectedResponse.id(), response.id());
            assertEquals("novyy-zagolovok", articleToSave.getSlug());
            verify(articleRepository).save(articleToSave);
        }
    }

    @Nested
    @DisplayName("updateArticle")
    class UpdateArticle {
        @Test
        @DisplayName("выбрасывает исключение, если статья не найдена")
        void shouldThrowNotFoundException() {
            UpdateArticleRequest request = new UpdateArticleRequest("T", "t", "S", "C", true);
            when(articleRepository.findById(99L)).thenReturn(Optional.empty());

            assertThrows(ResourceNotFoundException.class, () -> articleService.updateArticle(99L, request));
        }

        @Test
        @DisplayName("успешно обновляет статью")
        void shouldUpdateArticle() {
            Long articleId = 1L;
            UpdateArticleRequest request = new UpdateArticleRequest("New Title", "new-title", "New Summary", "New Content", true);
            Article existingArticle = new Article();
            existingArticle.setId(articleId);

            when(articleRepository.findById(articleId)).thenReturn(Optional.of(existingArticle));
            when(articleRepository.save(any(Article.class))).thenReturn(existingArticle);

            articleService.updateArticle(articleId, request);

            verify(articleMapper).updateFromRequest(request, existingArticle);
            verify(articleRepository).save(existingArticle);
            assertEquals("new-title", existingArticle.getSlug());
        }
    }

    @Nested
    @DisplayName("getActiveBySlug")
    class GetActiveBySlug {
        @Test
        @DisplayName("успешно находит активную статью")
        void shouldFindActiveArticleBySlug() {
            String slug = "test-slug";
            Article article = new Article();
            when(articleRepository.findBySlugAndActive(slug, true)).thenReturn(Optional.of(article));
            when(articleMapper.toResponse(article)).thenReturn(mock(ArticleResponse.class));

            articleService.getActiveBySlug(slug);

            verify(articleRepository).findBySlugAndActive(slug, true);
            verify(articleMapper).toResponse(article);
        }

        @Test
        @DisplayName("выбрасывает исключение, если активная статья не найдена")
        void shouldThrowNotFoundForInactiveArticle() {
            String slug = "test-slug";
            when(articleRepository.findBySlugAndActive(slug, true)).thenReturn(Optional.empty());

            assertThrows(ResourceNotFoundException.class, () -> articleService.getActiveBySlug(slug));
        }
    }

    @Nested
    @DisplayName("getActiveArticlesInfo (с проекциями)")
    class GetActiveArticlesInfo {
        @Test
        @DisplayName("возвращает страницу кратких статей, используя проекции")
        void shouldReturnPageOfArticleInfoUsingProjections() {
            // Given
            Pageable pageable = PageRequest.of(1, 10);
            ArticleInfoProjection projection = mock(ArticleInfoProjection.class);
            List<ArticleInfoProjection> projections = List.of(projection);
            Page<ArticleInfoProjection> projectionPage = new PageImpl<>(projections, pageable, 1);

            PageableResponse<ArticleInfoProjection> pageableResponse = PageableGenerator.generatePageableArticlesResponse(projectionPage.getContent());

            when(pageableResponseUtil.buildPageableResponse(any(), any(), any())).thenReturn(pageableResponse);
            when(articleRepository.findProjectionByActive(true, pageable)).thenReturn(projectionPage);
            when(articleMapper.toInfoResponse(any(ArticleInfoProjection.class))).thenReturn(mock(ArticleInfoResponse.class));

            // When
            PageableResponse<ArticleInfoResponse> result = articleService.getActiveArticlesInfo(pageable);

            // Then
            assertEquals(1, result.getPaginationInfo().getCurrentPage());
            verify(articleRepository).findProjectionByActive(true, pageable);
            verify(articleMapper, times(1)).toInfoResponse(any(ArticleInfoProjection.class));
        }
    }

    @Nested
    @DisplayName("getAllArticlesInfo (с проекциями)")
    class GetAllArticlesInfo {
        @Test
        @DisplayName("возвращает страницу всех кратких статей, используя проекции")
        void shouldReturnPageOfAllArticleInfoUsingProjections() {
            // Given
            Pageable pageable = PageRequest.of(1, 10);
            ArticleInfoProjection projection = mock(ArticleInfoProjection.class);
            List<ArticleInfoProjection> projections = List.of(projection);
            Page<ArticleInfoProjection> projectionPage = new PageImpl<>(projections, pageable, 1);

            PageableResponse<ArticleInfoProjection> pageableResponse = PageableGenerator.generatePageableArticlesResponse(projectionPage.getContent());

            when(pageableResponseUtil.buildPageableResponse(any(), any(), any())).thenReturn(pageableResponse);
            when(articleRepository.findAllProjectionBy(pageable)).thenReturn(projectionPage);
            when(articleMapper.toInfoResponse(any(ArticleInfoProjection.class))).thenReturn(mock(ArticleInfoResponse.class));

            // When
            PageableResponse<ArticleInfoResponse> result = articleService.getAllArticlesInfo(pageable);

            // Then
            assertEquals(1, result.getPaginationInfo().getTotalItems());
            verify(articleRepository).findAllProjectionBy(pageable);
            verify(articleRepository, never()).findAll(any(Pageable.class));
            verify(articleMapper, times(1)).toInfoResponse(any(ArticleInfoProjection.class));
        }
    }
}
