package software.pxel.learneasy.mapper;

import org.mapstruct.Builder;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;
import software.pxel.learneasy.api.dto.article.ArticleInfoResponse;
import software.pxel.learneasy.api.dto.article.ArticleResponse;
import software.pxel.learneasy.api.dto.article.CreateArticleRequest;
import software.pxel.learneasy.api.dto.article.UpdateArticleRequest;
import software.pxel.learneasy.model.Article;
import software.pxel.learneasy.repository.projection.ArticleInfoProjection;

@Mapper(componentModel = "spring", builder = @Builder(disableBuilder = true))
public interface ArticleMapper {

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "slug", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    Article fromCreateRequest(CreateArticleRequest request);

    ArticleResponse toResponse(Article article);

    ArticleInfoResponse toInfoResponse(ArticleInfoProjection projection);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    void updateFromRequest(UpdateArticleRequest request, @MappingTarget Article article);
}
