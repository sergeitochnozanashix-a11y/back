package software.pxel.learneasy.model.enums;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

@DisplayName("Period — разбор параметра period")
class PeriodTest {

    @Test
    @DisplayName("пустое значение даёт задокументированный дефолт month")
    void blankFallsBackToMonth() {
        assertEquals(Period.MONTH, Period.fromString(null));
        assertEquals(Period.MONTH, Period.fromString("   "));
    }

    @Test
    @DisplayName("регистр и пробелы не важны")
    void caseAndWhitespaceInsensitive() {
        assertEquals(Period.WEEK, Period.fromString(" WeEk "));
        assertEquals(Period.YEAR, Period.fromString("year"));
    }

    @Test
    @DisplayName("непустой мусор — ошибка, а не тихая подмена на month")
    void garbageThrows() {
        // Раньше возвращался MONTH: клиент получал данные не за тот период и
        // не мог об этом узнать.
        assertThrows(IllegalArgumentException.class, () -> Period.fromString("nonsense"));
    }

    @Test
    @DisplayName("allowedValues перечисляет значения в том виде, в каком их принимает API")
    void allowedValuesAreLowercase() {
        assertEquals("day, week, month, year", Period.allowedValues());
    }
}
