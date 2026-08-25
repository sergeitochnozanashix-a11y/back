package software.pxel.learneasy.api.dto.article;

import io.swagger.v3.oas.annotations.media.Schema;
import org.springframework.data.domain.PageImpl;

import java.util.Collections;

@Schema(name = "PageArticleInfo")
public class PageArticleInfo extends PageImpl<ArticleInfoResponse> {
    public PageArticleInfo() {
        super(Collections.emptyList());
    }
}
