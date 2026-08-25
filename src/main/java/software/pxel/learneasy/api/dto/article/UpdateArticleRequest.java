package software.pxel.learneasy.api.dto.article;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

@Schema(description = "Запрос на обновление статьи")
public record UpdateArticleRequest(
        @Schema(description = "Заголовок статьи", example = "Что такое SOLID?")
        @NotBlank
        @Size(max = 255)
        String title,

        @Schema(description = "Уникальный идентификатор для URL. Если не указан, будет сгенерирован из заголовка.", example = "chto-takoe-solid")
        @Size(max = 255)
        String slug,

        @Schema(description = "Краткое содержание для превью", example = "SOLID - это пять принципов...")
        String summary,

        @Schema(description = "Основной контент статьи в формате Markdown")
        @NotBlank
        String content,

        @Schema(description = "Статус активности. `true` для публикации, `false` для черновика.", example = "true")
        @NotNull
        Boolean active
) {
}
