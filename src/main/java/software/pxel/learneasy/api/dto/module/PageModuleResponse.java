package software.pxel.learneasy.api.dto.module;

import io.swagger.v3.oas.annotations.media.Schema;
import org.springframework.data.domain.PageImpl;

import java.util.Collections;

@Schema(name = "PageModuleResponse")
public class PageModuleResponse extends PageImpl<ModuleResponse> {
    public PageModuleResponse() {
        super(Collections.emptyList());
    }
}
