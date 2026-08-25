package software.pxel.learneasy.api.dto.content.properties;

import com.fasterxml.jackson.annotation.JsonInclude;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

@JsonInclude(JsonInclude.Include.NON_NULL)
@Getter
@Setter
public class ImageProperties extends CommonProperties {

    @NotNull(message = "Ширина изображения не может быть пустой")
    @Min(value = 1, message = "Ширина должна быть не меньше 1")
    private Integer width;

    @NotNull(message = "Высота изображения не может быть пустой")
    @Min(value = 1, message = "Высота должна быть не меньше 1")
    private Integer height;

    @Schema(nullable = true, description = "Альтернативный текст для изображения")
    @NotBlank(message = "Альтернативный текст не может быть пустым")
    private String alt;

    @Schema(nullable = true, description = "Подпись к изображению")
    private String caption;

    public ImageProperties(String color, Boolean inline, int mb,
                           Integer width, Integer height, String alt, String caption) {
        super(color, inline, mb);
        this.width = width;
        this.height = height;
        this.alt = alt;
        this.caption = caption;
    }

    public ImageProperties() {
        super();
    }
}
