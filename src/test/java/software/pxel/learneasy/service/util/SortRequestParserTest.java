package software.pxel.learneasy.service.util;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.Sort;
import software.pxel.learneasy.exception.BadRequestException;

import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

@DisplayName("SortRequestParser — разбор параметра sort по белому списку")
class SortRequestParserTest {

    private static final Set<String> ALLOWED = Set.of("id", "title", "createdAt");
    private static final Sort DEFAULT_SORT = Sort.by(Sort.Direction.ASC, "title");

    @Test
    @DisplayName("успех — поле из белого списка и направление asc")
    void success_allowedFieldAsc() {
        Sort sort = SortRequestParser.parse("title,asc", ALLOWED, DEFAULT_SORT);

        Sort.Order order = sort.getOrderFor("title");
        assertEquals(Sort.Direction.ASC, order.getDirection());
    }

    @Test
    @DisplayName("успех — направление desc и регистр направления не важен")
    void success_directionCaseInsensitive() {
        assertEquals(Sort.Direction.DESC,
                SortRequestParser.parse("createdAt,DESC", ALLOWED, DEFAULT_SORT).getOrderFor("createdAt").getDirection());
    }

    @Test
    @DisplayName("успех — пробелы вокруг значений срезаются")
    void success_trimsWhitespace() {
        Sort sort = SortRequestParser.parse(" id , desc ", ALLOWED, DEFAULT_SORT);

        assertEquals(Sort.Direction.DESC, sort.getOrderFor("id").getDirection());
    }

    @Test
    @DisplayName("успех — пустой параметр даёт сортировку по умолчанию")
    void success_blankFallsBackToDefault() {
        assertEquals(DEFAULT_SORT, SortRequestParser.parse(null, ALLOWED, DEFAULT_SORT));
        assertEquals(DEFAULT_SORT, SortRequestParser.parse("   ", ALLOWED, DEFAULT_SORT));
    }

    @Test
    @DisplayName("ошибка — неизвестное поле даёт 400, а не падение внутри Spring Data")
    void error_unknownField() {
        BadRequestException ex = assertThrows(BadRequestException.class,
                () -> SortRequestParser.parse("nosuchfield,asc", ALLOWED, DEFAULT_SORT));

        assertTrue(ex.getMessage().contains("nosuchfield"), ex.getMessage());
        // Сообщение должно подсказывать, что допустимо.
        assertTrue(ex.getMessage().contains("title"), ex.getMessage());
    }

    @Test
    @DisplayName("ошибка — недопустимое направление")
    void error_unknownDirection() {
        BadRequestException ex = assertThrows(BadRequestException.class,
                () -> SortRequestParser.parse("title,sideways", ALLOWED, DEFAULT_SORT));

        assertTrue(ex.getMessage().contains("asc, desc"), ex.getMessage());
    }

    @Test
    @DisplayName("ошибка — нарушен формат 'поле,направление'")
    void error_malformed() {
        assertThrows(BadRequestException.class, () -> SortRequestParser.parse("title", ALLOWED, DEFAULT_SORT));
        assertThrows(BadRequestException.class, () -> SortRequestParser.parse("title,", ALLOWED, DEFAULT_SORT));
        assertThrows(BadRequestException.class, () -> SortRequestParser.parse(",asc", ALLOWED, DEFAULT_SORT));
        assertThrows(BadRequestException.class, () -> SortRequestParser.parse("a,b,c", ALLOWED, DEFAULT_SORT));
    }

    @Test
    @DisplayName("ошибка — попытка сортировки по вложенному свойству отклоняется")
    void error_nestedPropertyRejected() {
        // Spring Data приняла бы "course.title" и ушла в join по чужой сущности.
        assertThrows(BadRequestException.class,
                () -> SortRequestParser.parse("course.title,asc", ALLOWED, DEFAULT_SORT));
    }
}
