package software.pxel.learneasy.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import software.pxel.learneasy.model.Article;
import software.pxel.learneasy.repository.projection.ArticleInfoProjection;

import java.util.Optional;

@Repository
public interface ArticleRepository extends JpaRepository<Article, Long> {

    Optional<Article> findBySlug(String slug);

    Optional<Article> findBySlugAndActive(String slug, Boolean active);

    Page<ArticleInfoProjection> findProjectionByActive(boolean active, Pageable pageable);

    Page<ArticleInfoProjection> findAllProjectionBy(Pageable pageable);
}
