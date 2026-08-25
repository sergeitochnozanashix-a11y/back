package software.pxel.learneasy.api.dto.content.properties;

import com.fasterxml.jackson.annotation.JsonInclude;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

@JsonInclude(JsonInclude.Include.NON_NULL)
@Getter
@Setter
public class LinkProperties extends CommonProperties {

    @Schema(description = "Ссылка")
    @NotBlank(message = "URL ссылки не может быть пустым")
    private String url;

    @Schema(nullable = true, description = "Целевое окно для ссылки")
    private String target; // _self | _blank | _parent | _top

    public LinkProperties(String color, Boolean inline, Integer mb,
                          String url, String target) {
        super(color, inline, mb);
        this.url = url;
        this.target = target;
    }

    public LinkProperties() {
        super();
    }
}
