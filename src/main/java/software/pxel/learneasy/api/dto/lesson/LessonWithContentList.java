package software.pxel.learneasy.api.dto.lesson;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import software.pxel.learneasy.api.dto.content.CreateBlockBaseDTO;

import java.util.List;

@Builder
@Schema(description = "Модель ответа для получения урока с содержанием")
public record LessonWithContentList(
        @Schema(description = "Уникальный номер урока", example = "1") Long id,
        @Schema(description = "Название урока", example = "Web Development Fundamentals") String title,
        @Schema(description = "Описание урока", example = "Understand HTML, CSS, and JavaScript to build web pages.") String description,
        @Schema(description = "Порядковый номер урока в модуле", example = "1") Integer sequenceOrder,
        @Schema(description = "Количество вопросов в тесте, привязанном к уроку. null — если теста нет") Integer testQuestions,
        @Schema(description = "Порядковый номер модуля в курсе", example = "1") Integer moduleSequenceOrder,
        @Schema(description = "Содержимое урока в формате Markdown") String content,
        @Schema(description = "Массив содержания (старый формат, JSON)") List<CreateBlockBaseDTO> contentBlocks
) {
}
