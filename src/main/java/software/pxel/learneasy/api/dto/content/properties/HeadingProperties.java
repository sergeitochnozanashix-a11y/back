package software.pxel.learneasy.api.dto.content.properties;

import com.fasterxml.jackson.annotation.JsonInclude;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.Getter;
import lombok.Setter;

@JsonInclude(JsonInclude.Include.NON_NULL)
@Getter
@Setter
public class HeadingProperties extends CommonProperties {
    @Schema(name = "Уровень заголовка")
    @Min(value = 1, message = "Уровень заголовка должен быть не меньше 1")
    @Max(value = 6, message = "Уровень заголовка должен быть не больше 6")
    int level;

    public HeadingProperties(String color, Boolean inline, int mb, int level) {
        super(color, inline, mb);
        this.level = level;
    }

    public HeadingProperties() {
        super();
    }
}
