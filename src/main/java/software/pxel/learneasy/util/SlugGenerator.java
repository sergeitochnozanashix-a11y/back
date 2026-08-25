package software.pxel.learneasy.util;

import java.text.Normalizer;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Pattern;

public final class SlugGenerator {

    private static final Map<Character, String> CYRILLIC_TO_LATIN_MAP = new HashMap<>();
    private static final Pattern NON_LATIN = Pattern.compile("[^\\w-]");
    private static final Pattern WHITESPACE = Pattern.compile("[\\s]");
    private static final Pattern MULTIPLE_HYPHENS = Pattern.compile("-{2,}");

    static {
        CYRILLIC_TO_LATIN_MAP.put('а', "a");
        CYRILLIC_TO_LATIN_MAP.put('б', "b");
        CYRILLIC_TO_LATIN_MAP.put('в', "v");
        CYRILLIC_TO_LATIN_MAP.put('г', "g");
        CYRILLIC_TO_LATIN_MAP.put('д', "d");
        CYRILLIC_TO_LATIN_MAP.put('е', "e");
        CYRILLIC_TO_LATIN_MAP.put('ё', "yo");
        CYRILLIC_TO_LATIN_MAP.put('ж', "zh");
        CYRILLIC_TO_LATIN_MAP.put('з', "z");
        CYRILLIC_TO_LATIN_MAP.put('и', "i");
        CYRILLIC_TO_LATIN_MAP.put('й', "y");
        CYRILLIC_TO_LATIN_MAP.put('к', "k");
        CYRILLIC_TO_LATIN_MAP.put('л', "l");
        CYRILLIC_TO_LATIN_MAP.put('м', "m");
        CYRILLIC_TO_LATIN_MAP.put('н', "n");
        CYRILLIC_TO_LATIN_MAP.put('о', "o");
        CYRILLIC_TO_LATIN_MAP.put('п', "p");
        CYRILLIC_TO_LATIN_MAP.put('р', "r");
        CYRILLIC_TO_LATIN_MAP.put('с', "s");
        CYRILLIC_TO_LATIN_MAP.put('т', "t");
        CYRILLIC_TO_LATIN_MAP.put('у', "u");
        CYRILLIC_TO_LATIN_MAP.put('ф', "f");
        CYRILLIC_TO_LATIN_MAP.put('х', "kh");
        CYRILLIC_TO_LATIN_MAP.put('ц', "ts");
        CYRILLIC_TO_LATIN_MAP.put('ч', "ch");
        CYRILLIC_TO_LATIN_MAP.put('ш', "sh");
        CYRILLIC_TO_LATIN_MAP.put('щ', "shch");
        CYRILLIC_TO_LATIN_MAP.put('ъ', "");
        CYRILLIC_TO_LATIN_MAP.put('ы', "y");
        CYRILLIC_TO_LATIN_MAP.put('ь', "");
        CYRILLIC_TO_LATIN_MAP.put('э', "e");
        CYRILLIC_TO_LATIN_MAP.put('ю', "yu");
        CYRILLIC_TO_LATIN_MAP.put('я', "ya");
    }

    private SlugGenerator() {
        throw new IllegalStateException("Utility class");
    }

    public static String toSlug(String input) {
        if (input == null || input.isBlank()) {
            return "";
        }

        StringBuilder transliterated = new StringBuilder();
        input.toLowerCase().chars()
                .mapToObj(c -> (char) c)
                .forEach(c -> transliterated.append(CYRILLIC_TO_LATIN_MAP.getOrDefault(c, String.valueOf(c))));

        String noWhitespace = WHITESPACE.matcher(transliterated.toString()).replaceAll("-");
        String normalized = Normalizer.normalize(noWhitespace, Normalizer.Form.NFD);
        String slug = NON_LATIN.matcher(normalized).replaceAll("");
        slug = MULTIPLE_HYPHENS.matcher(slug).replaceAll("-");

        if (slug.startsWith("-")) {
            slug = slug.substring(1);
        }
        if (slug.endsWith("-")) {
            slug = slug.substring(0, slug.length() - 1);
        }

        return slug;
    }
}
