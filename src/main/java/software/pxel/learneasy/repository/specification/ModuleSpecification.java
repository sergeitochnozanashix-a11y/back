package software.pxel.learneasy.repository.specification;

import jakarta.persistence.criteria.Predicate;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.util.StringUtils;
import software.pxel.learneasy.model.Module;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public class ModuleSpecification {

    private ModuleSpecification() {
        throw new IllegalStateException("Util class");
    }

    public static Specification<Module> filterSpecification(String title,
                                                            String description,
                                                            Long courseId,
                                                            LocalDateTime createdAfter,
                                                            LocalDateTime createdBefore) {

        return (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();

            if (StringUtils.hasText(title)) {
                predicates.add(cb.like(
                        cb.lower(root.get("title")),
                        "%" + title.toLowerCase() + "%"
                ));
            }

            if (StringUtils.hasText(description)) {
                predicates.add(cb.like(
                        cb.lower(root.get("description")),
                        "%" + description.toLowerCase() + "%"
                ));
            }

            if (courseId != null) {
                predicates.add(cb.equal(
                        root.get("course").get("id"),
                        courseId
                ));
            }

            if (createdAfter != null) {
                predicates.add(cb.greaterThanOrEqualTo(
                        root.get("createdAt"),
                        createdAfter
                ));
            }

            if (createdBefore != null) {
                predicates.add(cb.lessThanOrEqualTo(
                        root.get("createdAt"),
                        createdBefore
                ));
            }

            return cb.and(predicates.toArray(new Predicate[0]));
        };
    }
}
