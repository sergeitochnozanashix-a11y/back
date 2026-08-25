package software.pxel.learneasy.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import software.pxel.learneasy.model.TestModel;
import software.pxel.learneasy.model.enums.TestType;
import software.pxel.learneasy.repository.projection.TestModelWithQuestionsCount;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.Set;

public interface TestModelRepository extends JpaRepository<TestModel, Long> {

    @Query("SELECT t FROM TestModel t LEFT JOIN FETCH t.questions q WHERE t.id = :id")
    Optional<TestModel> findByIdWithQuestions(Long id);

    @Query("SELECT t FROM TestModel t LEFT JOIN FETCH t.questions q WHERE t.lesson.id = :lessonId")
    Optional<TestModel> findByLessonIdWithQuestions(Long lessonId);

    Optional<TestModel> findByLessonId(Long lessonId);

    boolean existsByLessonId(Long newLessonId);

    boolean existsByLessonIdAndModuleId(Long lessonId, Long moduleId);

    @Query("SELECT t.id AS id, t.title AS title, t.module.id AS moduleId, t.module.title AS moduleTitle, " +
            "t.module.sequenceOrder AS moduleSequenceOrder, " +
            "SIZE(t.questions) AS questionsCount, t.passThresholdPercentage AS passThresholdPercentage " +
            "FROM TestModel t WHERE t.module.course.id = :courseId AND t.testType = :testType")
    Page<TestModelWithQuestionsCount> findByCourseIdAndTestType(@Param("courseId") Long courseId, @Param("testType") TestType testType, Pageable pageable);

    @Query("""
            SELECT COUNT(q)
            FROM Question q
            WHERE q.test.lesson.id = :lessonId
            """)
    int countQuestionsByLessonId(@Param("lessonId") Long lessonId);

    @Query("""
                SELECT t.lesson.module.id as moduleId, t.lesson.id as lessonId
                FROM TestModel t
                WHERE t.lesson.id IS NOT NULL AND t.lesson.module.course.id = :courseId
            """)
    List<ModuleLessonPair> findModuleLessonPairsWithTestsByCourseId(@Param("courseId") Long courseId);

    @Query("""
            SELECT t.lesson.id
            FROM TestModel t
            WHERE t.lesson.id IS NOT NULL
              AND t.lesson.module.id IN :moduleIds
            """)
    Set<Long> findLessonIdsWithTestsByModuleIds(@Param("moduleIds") Collection<Long> moduleIds);

    interface ModuleLessonPair {
        Long getModuleId();

        Long getLessonId();
    }
}
