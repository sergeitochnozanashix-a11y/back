package software.pxel.learneasy.repository.projection;

import java.time.Instant;

public interface ArticleInfoProjection {
    Long getId();

    String getTitle();

    String getSlug();

    String getSummary();

    Boolean getActive();

    Instant getCreatedAt();

    Instant getUpdatedAt();
}
