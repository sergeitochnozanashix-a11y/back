package software.pxel.learneasy.api.dto.pageable;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
@Schema(description = "Общий ответ с пагинацией")
public class PaginationInfo {

    @Schema(description = "Текущая страница", example = "1")
    int currentPage;

    @Schema(description = "Количество элементов на странице", example = "3")
    int itemsPerPage;

    @Schema(description = "Всего страниц", example = "1")
    int totalPages;

    @Schema(description = "Совокупные элементы", example = "3")
    long totalItems;
}
