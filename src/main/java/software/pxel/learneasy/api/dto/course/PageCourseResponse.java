package software.pxel.learneasy.api.dto.course;

import io.swagger.v3.oas.annotations.media.Schema;
import org.springframework.data.domain.PageImpl;

import java.util.Collections;

@Schema(name = "PageCourseResponse")
public class PageCourseResponse extends PageImpl<CourseResponse> {
    public PageCourseResponse() {
        super(Collections.emptyList());
    }
}
