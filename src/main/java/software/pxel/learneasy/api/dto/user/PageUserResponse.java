package software.pxel.learneasy.api.dto.user;

import io.swagger.v3.oas.annotations.media.Schema;
import org.springframework.data.domain.PageImpl;

import java.util.Collections;

@Schema(name = "PageUserResponse")
public class PageUserResponse extends PageImpl<UserDTO> {
    public PageUserResponse() {
        super(Collections.emptyList());
    }
}
