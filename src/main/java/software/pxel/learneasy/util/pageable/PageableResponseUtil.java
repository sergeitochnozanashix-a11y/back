package software.pxel.learneasy.util.pageable;

import org.springframework.data.domain.Page;
import org.springframework.stereotype.Component;
import software.pxel.learneasy.api.dto.pageable.PageableResponse;
import software.pxel.learneasy.api.dto.pageable.PaginationInfo;

import java.util.List;

@Component
public class PageableResponseUtil {

    public <T, R extends PageableResponse<T>> R buildPageableResponse(
            List<T> content, Page<?> page, R response
    ) {
        response.setContent(content);
        response.setPaginationInfo(
                PaginationInfo.builder()
                        .currentPage(page.getNumber() + 1)
                        .itemsPerPage(page.getNumberOfElements())
                        .totalPages(page.getTotalPages())
                        .totalItems(page.getTotalElements())
                        .build());

        return response;
    }
}
