package software.pxel.learneasy.api.dto.content.properties;

import com.fasterxml.jackson.annotation.JsonInclude;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Pattern;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@JsonInclude(JsonInclude.Include.NON_NULL)
@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
public class CommonProperties {
    @Schema(description = "Цвет в формате HEX, например #231542", nullable = true)
    @Pattern(regexp = "^#[0-9a-fA-F]{6}$", message = "Цвет должен быть в формате HEX, например #231542")
    String color;

    @Schema(description = "Встраиваемый ли блок", nullable = true)
    Boolean inline;

    @Schema(description = "Нижний отступ в пикселях", nullable = true)
    Integer mb;
}
