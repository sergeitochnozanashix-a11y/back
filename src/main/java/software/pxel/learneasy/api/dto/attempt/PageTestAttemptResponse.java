package software.pxel.learneasy.api.dto.attempt;

import io.swagger.v3.oas.annotations.media.Schema;
import org.springframework.data.domain.PageImpl;

import java.util.Collections;

@Schema(name = "PageTestAttemptResponse")
public class PageTestAttemptResponse extends PageImpl<TestAttemptResponse> {
    public PageTestAttemptResponse() {
        super(Collections.emptyList());
    }
}
