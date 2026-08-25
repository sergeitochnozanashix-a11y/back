package software.pxel.learneasy.utilgenerator;

import lombok.experimental.UtilityClass;
import software.pxel.learneasy.api.dto.pageable.PageableResponse;
import software.pxel.learneasy.api.dto.pageable.PaginationInfo;
import software.pxel.learneasy.repository.projection.ArticleInfoProjection;

import java.util.List;

@UtilityClass
public class PageableGenerator {

    public static PageableResponse<ArticleInfoProjection> generatePageableArticlesResponse(List<ArticleInfoProjection> content) {
        return PageableResponse.<ArticleInfoProjection>builder()
                .content(content)
                .paginationInfo(PaginationInfo.builder()
                        .currentPage(1)
                        .itemsPerPage(content.size())
                        .totalPages(1)
                        .totalItems(content.size())
                        .build())
                .build();
    }
}
