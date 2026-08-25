package software.pxel.learneasy.api.dto.pageable;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
@Schema(description = "Общий ответ с пагинацией")
public class PageableResponse<T> {

    @Schema(description = "Список данных", example = "data[]")
    private List<T> content;

    @Schema(description = "Информация о пагинации", example = """
            "paginationInfo": {
                    "currentPage": 1,
                    "itemsPerPage": 3,
                    "totalPages": 1,
                    "totalItems": 3
                }
            """)
    private PaginationInfo paginationInfo;
}
