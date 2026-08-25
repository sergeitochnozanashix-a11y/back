package software.pxel.learneasy.api.dto.article;

import io.swagger.v3.oas.annotations.media.Schema;

import java.time.Instant;

@Schema(description = "DTO для отображения статьи")
public record ArticleResponse(
        @Schema(description = "Уникальный идентификатор", example = "1")
        Long id,

        @Schema(description = "Заголовок статьи", example = "Что такое SOLID?")
        String title,

        @Schema(description = "Уникальный идентификатор для URL", example = "chto-takoe-solid")
        String slug,

        @Schema(description = "Краткое содержание для превью", example = "SOLID - это пять принципов...")
        String summary,

        @Schema(description = "Основной контент статьи в формате Markdown")
        String content,

        @Schema(description = "Статус активности (true - опубликована, false - черновик)", example = "true")
        Boolean active,

        @Schema(description = "Дата создания", example = "2025-10-19T18:30:00Z")
        Instant createdAt,

        @Schema(description = "Дата последнего обновления", example = "2025-10-19T19:00:00Z")
        Instant updatedAt
) {
}
