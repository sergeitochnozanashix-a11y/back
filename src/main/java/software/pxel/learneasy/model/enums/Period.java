package software.pxel.learneasy.model.enums;

import java.util.Locale;

public enum Period {
    DAY, WEEK, MONTH, YEAR;

    /**
     * Пустое значение даёт {@link #MONTH} - это задокументированный дефолт
     * параметра. А вот непустой мусор раньше тоже молча превращался в MONTH:
     * клиент получал данные не за тот период и никак об этом не узнавал.
     * Теперь такой случай - ошибка, которую контроллер превращает в 400.
     *
     * @throws IllegalArgumentException если значение задано, но не входит в набор
     */
    public static Period fromString(String value) {
        if (value == null || value.isBlank()) return MONTH;
        return Period.valueOf(value.trim().toUpperCase(Locale.ROOT));
    }

    /**
     * Допустимые значения в том виде, в каком их принимает API.
     */
    public static String allowedValues() {
        return java.util.Arrays.stream(values())
                .map(Period::toString)
                .collect(java.util.stream.Collectors.joining(", "));
    }

    @Override
    public String toString() {
        return name().toLowerCase();
    }
}
