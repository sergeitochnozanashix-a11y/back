package software.pxel.learneasy.service.util;

import org.springframework.data.domain.Sort;
import software.pxel.learneasy.exception.BadRequestException;

import java.util.Set;

/**
 * Разбор параметра {@code sort=поле,направление} с проверкой имени поля по
 * белому списку.
 * <p>
 * Без такой проверки значение из запроса уходило напрямую в {@code Sort.by()},
 * Spring Data не находила свойство и бросала {@code PropertyReferenceException}
 * уже на этапе выполнения запроса - клиент получал 500 вместо 400 на собственную
 * опечатку. Белый список нужен именно по имени поля: перечислять допустимые
 * значения безопаснее, чем пытаться отфильтровать недопустимые.
 */
public final class SortRequestParser {

    private SortRequestParser() {
        throw new IllegalStateException("Утилитный класс не предназначен для создания экземпляров");
    }

    /**
     * @param sort               сырое значение параметра, например {@code "title,asc"}
     * @param allowedProperties  имена свойств, по которым разрешена сортировка
     * @param defaultSort        значение для пустого параметра
     * @throws BadRequestException если формат нарушен, поле не входит в белый
     *                             список или направление отличается от asc/desc
     */
    public static Sort parse(String sort, Set<String> allowedProperties, Sort defaultSort) {
        if (sort == null || sort.isBlank()) {
            return defaultSort;
        }

        String[] parts = sort.split(",");
        if (parts.length != 2 || parts[0].isBlank() || parts[1].isBlank()) {
            throw new BadRequestException(
                    "Некорректный формат параметра sort: '" + sort + "'. Ожидается 'поле,направление', "
                            + "например 'title,asc'.");
        }

        String property = parts[0].trim();
        String direction = parts[1].trim();

        if (!allowedProperties.contains(property)) {
            throw new BadRequestException(
                    "Недопустимое поле сортировки: '" + property + "'. Допустимые значения: "
                            + String.join(", ", allowedProperties.stream().sorted().toList()) + ".");
        }

        Sort.Direction parsedDirection;
        try {
            parsedDirection = Sort.Direction.fromString(direction);
        } catch (IllegalArgumentException e) {
            throw new BadRequestException(
                    "Недопустимое направление сортировки: '" + direction + "'. Допустимые значения: asc, desc.");
        }

        return Sort.by(parsedDirection, property);
    }
}
