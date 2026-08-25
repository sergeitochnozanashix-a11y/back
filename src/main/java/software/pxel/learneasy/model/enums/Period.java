package software.pxel.learneasy.model.enums;

import java.util.Locale;

public enum Period {
    DAY, WEEK, MONTH, YEAR;

    public static Period fromString(String value) {
        if (value == null || value.isBlank()) return MONTH;
        try {
            return Period.valueOf(value.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException e) {
            return MONTH;
        }
    }

    @Override
    public String toString() {
        return name().toLowerCase();
    }
}
