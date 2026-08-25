package software.pxel.learneasy.api.dto.content.properties;

import com.fasterxml.jackson.annotation.JsonInclude;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.Setter;

@JsonInclude(JsonInclude.Include.NON_NULL)
@Getter
@Setter
public class TextProperties extends CommonProperties {

    @Schema(nullable = true, description = "Стиль текста")
    private String style; // bold | italic | underline | bold italic

    @Schema(nullable = true, description = "Размер текста")
    private String size; // s | m | l

    public TextProperties(String color, Boolean inline, Integer mb,
                          String style, String size) {
        super(color, inline, mb);
        this.style = style;
        this.size = size;
    }

    public TextProperties() {
        super();
    }
}
