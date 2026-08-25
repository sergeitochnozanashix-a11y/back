package software.pxel.learneasy.api.dto.lesson;

import io.swagger.v3.oas.annotations.media.Schema;
import org.springframework.data.domain.PageImpl;

import java.util.Collections;

@Schema(name = "PageLessonResponse")
public class PageLessonWithoutContent extends PageImpl<LessonWithoutContentList> {
    public PageLessonWithoutContent() {
        super(Collections.emptyList());
    }
}
