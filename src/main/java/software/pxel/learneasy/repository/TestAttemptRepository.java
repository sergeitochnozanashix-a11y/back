package software.pxel.learneasy.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import software.pxel.learneasy.api.dto.admin.projection.UserProgressProjection;
import software.pxel.learneasy.model.TestAttempt;

import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.Set;

@Repository
public interface TestAttemptRepository extends JpaRepository<TestAttempt, Long> {

    Optional<TestAttempt> findFirstByUserIdAndTest_IdOrderByCreatedAtDesc(Long userId, Long testId);

    Page<TestAttempt> findByUserIdAndTest_IdOrderByCreatedAtDesc(Long userId, Long testId, Pageable pageable);

    @Query("SELECT ta FROM TestAttempt ta JOIN FETCH ta.test WHERE ta.id = :attemptId")
    Optional<TestAttempt> findByIdWithTest(Long attemptId);

    /**
     * Ежедневно сданные тесты (attempts с passed = true) за период по UTC.
     * Возвращает список [java.sql.Date day, Long count]
     */
    @Query("""
            SELECT date(ta.createdAt) AS day, COUNT(ta) AS cnt
            FROM TestAttempt ta
            WHERE ta.userId = :userId
              AND ta.passed = true
              AND ta.createdAt BETWEEN :start AND :end
            GROUP BY date(ta.createdAt)
            ORDER BY day
            """)
    List<Object[]> findDailyTestStats(Long userId, Instant start, Instant end);

    Optional<TestAttempt> findFirstByUserIdAndTestIdOrderByCreatedAtDesc(Long userId, Long id);

    Optional<TestAttempt> findFirstByUserIdAndTestLessonIdOrderByCreatedAtDesc(Long userId, Long lessonId);

    @Query("""
            SELECT DISTINCT ta.lesson.id
            FROM TestAttempt ta
            WHERE ta.userId = :userId
              AND ta.lesson.module.course.id = :courseId
              AND ta.passed = true
            """)
    Set<Long> findPassedLessonIdsByUserIdAndCourseId(@Param("userId") Long userId, @Param("courseId") Long courseId);

    @Query("""
            SELECT COUNT(DISTINCT ta.lesson.id)
            FROM TestAttempt ta
            WHERE ta.userId = :userId
              AND ta.lesson.module.course.id = :courseId
              AND ta.passed = true
              AND ta.lesson.id IS NOT NULL
            """)
    long countDistinctPassedLessonsByUserIdAndCourseId(@Param("userId") Long userId, @Param("courseId") Long courseId);

    @Query("""
            SELECT ta.lesson.id
            FROM TestAttempt ta
            WHERE ta.userId = :userId
              AND ta.lesson.module.id IN :moduleIds
              AND ta.passed = true
              AND ta.lesson.id IS NOT NULL
            """)
    Set<Long> findPassedLessonIdsByUserIdAndModuleIds(@Param("userId") Long userId, @Param("moduleIds") Collection<Long> moduleIds);


    @Query(value = """
            WITH LatestUserActivity AS (
                SELECT user_id, MAX(created_at) as last_activity
                FROM test_attempts
                WHERE passed = true
                GROUP BY user_id
                ORDER BY last_activity DESC
                LIMIT 10
            ),
            CourseLessonsWithTests AS (
                SELECT l.id as lesson_id, m.id as module_id, m.title as module_title, m.sequence_order as module_order
                FROM lessons l
                JOIN modules m ON l.module_id = m.id
                WHERE m.course_id = :courseId AND EXISTS (SELECT 1 FROM tests t WHERE t.lesson_id = l.id)
            ),
            UserProgress AS (
                SELECT
                    lua.user_id,
                    COUNT(DISTINCT t.lesson_id) FILTER (WHERE ta.passed = true) as total_passed_lessons
                FROM LatestUserActivity lua
                JOIN test_attempts ta ON lua.user_id = ta.user_id
                JOIN tests t ON ta.test_id = t.id
                WHERE t.lesson_id IN (SELECT lesson_id FROM CourseLessonsWithTests)
                GROUP BY lua.user_id
            ),
            RankedNextModules AS (
                SELECT
                    up.user_id,
                    clwt.module_title,
                    clwt.module_order,
                    (SELECT COUNT(DISTINCT t_inner.lesson_id)
                     FROM test_attempts ta_inner
                     JOIN tests t_inner ON ta_inner.test_id = t_inner.id
                     WHERE ta_inner.user_id = up.user_id AND t_inner.module_id = clwt.module_id AND ta_inner.passed = true) as passed_in_module,
                    ROW_NUMBER() OVER(PARTITION BY up.user_id ORDER BY clwt.module_order ASC) as rn
                FROM UserProgress up
                CROSS JOIN CourseLessonsWithTests clwt
                WHERE
                    (SELECT COUNT(*) FROM CourseLessonsWithTests WHERE module_id = clwt.module_id) >
                    (SELECT COUNT(DISTINCT t_inner.lesson_id)
                     FROM test_attempts ta_inner
                     JOIN tests t_inner ON ta_inner.test_id = t_inner.id
                     WHERE ta_inner.user_id = up.user_id AND t_inner.module_id = clwt.module_id AND ta_inner.passed = true)
            ),
            NextModuleForUser AS (
                SELECT
                    user_id,
                    module_title as next_module_name,
                    module_order as next_module_order,
                    passed_in_module as passed_in_next_module
                FROM RankedNextModules
                WHERE rn = 1
            )
            SELECT
                u.id                                                as userId,
                u.username                                          as fullName,
                up.total_passed_lessons                             as totalPassedLessons,
                (SELECT COUNT(*) FROM CourseLessonsWithTests)       as totalLessonsWithTests,
                nm.next_module_name as nextModuleName,
                nm.next_module_order as nextModuleOrder,
                nm.passed_in_next_module as passedInNextModule
            FROM users u
            JOIN LatestUserActivity lua ON u.id = lua.user_id
            LEFT JOIN UserProgress up ON u.id = up.user_id
            LEFT JOIN NextModuleForUser nm ON u.id = nm.user_id
            ORDER BY lua.last_activity DESC
            """, nativeQuery = true)
    List<UserProgressProjection> findUserProgressForDashboard(@Param("courseId") Long courseId);
}
