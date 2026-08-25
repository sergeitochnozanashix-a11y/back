package software.pxel.learneasy.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import software.pxel.learneasy.model.Module;
import software.pxel.learneasy.repository.projection.ModuleWithLessonCountsProjection;

import java.util.List;
import java.util.Optional;

public interface ModuleRepository extends FilterableRepository<Module, Long> {

    @Query("SELECT m FROM Module m LEFT JOIN FETCH m.lessons l where m.id = :moduleId")
    Optional<Module> findByIdWithLessons(Long moduleId);

    // TODO Заменить на @Query()
    @EntityGraph(attributePaths = {"lessons"})
    Page<Module> findAll(Specification<Module> spec, Pageable pageable);

    @EntityGraph(attributePaths = {"lessons"})
    List<Module> findAllByCourseId(Long courseId);

    @Query("SELECT m.sequenceOrder FROM Module m WHERE m.course.id = :courseId ORDER BY m.sequenceOrder ASC")
    List<Integer> findAllSequenceOrderByCourseId(Long courseId);

    @Query("SELECT m.sequenceOrder FROM Module m where m.id = :id")
    Integer findSequenceOrderById(Long id);

    @Query("""
            SELECT m.id AS moduleId,
                   m.title AS title,
                   m.description AS description,
                   COUNT(l) AS lessonCount,
                   m.sequenceOrder AS sequenceOrder
            FROM Module m
            LEFT JOIN m.lessons l
            WHERE m.course.id = :courseId
            GROUP BY m.id, m.title, m.description, m.sequenceOrder
            ORDER BY m.sequenceOrder ASC, m.id ASC
            """)
    List<ModuleWithLessonCountsProjection> findModulesWithLessonCountByCourseId(@Param("courseId") Long courseId);
}
