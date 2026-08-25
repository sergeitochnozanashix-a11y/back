package software.pxel.learneasy.api.dto.content.properties;

import com.fasterxml.jackson.annotation.JsonInclude;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

@JsonInclude(JsonInclude.Include.NON_NULL)
@Getter
@Setter
public class CodeProperties extends CommonProperties {

    @NotBlank(message = "Язык программирования не может быть пустым")
    private String language;

    @Schema(nullable = true, description = "Отображать ли номера строк")
    private Boolean lineNumbers;

    public CodeProperties(String color, Boolean inline, Integer mb,
                          String language, Boolean lineNumbers) {
        super(color, inline, mb);
        this.language = language;
        this.lineNumbers = lineNumbers;
    }

    public CodeProperties() {
        super();
    }
}
