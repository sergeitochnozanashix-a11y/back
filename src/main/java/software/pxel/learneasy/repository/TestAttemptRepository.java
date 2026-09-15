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

    /**
     * Курсы пользователя, упорядоченные по свежести активности. Первый элемент -
     * курс, в котором он занимался последним; его и показывает главная страница.
     * <p>
     * Идём через {@code ta.module}, а не через {@code ta.lesson}: module_id в
     * test_attempts объявлен NOT NULL, а lesson_id допускает null, и попытка без
     * урока выпала бы из выборки.
     * <p>
     * Отдельной записи «пользователь ↔ курс» в модели нет, поэтому активность -
     * единственный доступный признак принадлежности к курсу.
     */
    @Query("""
            SELECT ta.module.course.id
            FROM TestAttempt ta
            WHERE ta.userId = :userId
            ORDER BY ta.createdAt DESC
            """)
    List<Long> findLastActiveCourseIds(@Param("userId") Long userId, Pageable pageable);

    @Query("""
            SELECT ta.lesson.id
            FROM TestAttempt ta
            WHERE ta.userId = :userId
              AND ta.lesson.module.id IN :moduleIds
              AND ta.passed = true
              AND ta.lesson.id IS NOT NULL
            """)
    Set<Long> findPassedLessonIdsByUserIdAndModuleIds(@Param("userId") Long userId, @Param("moduleIds") Collection<Long> moduleIds);


    /**
     * Строки таблицы активности для админского дашборда.
     * <p>
     * Прогресс каждого пользователя считается по <b>его собственному</b> курсу -
     * тому, в котором у него последняя попытка теста. Раньше сюда передавался
     * courseId, захардкоженный в сервисе единицей: курса с таким id в базе не
     * было, поэтому у всех показывался прогресс 0% и пустой текущий модуль.
     * <p>
     * Пользователи без активности тоже попадают в выборку (LEFT JOIN) - иначе
     * длина таблицы расходилась с totalUsers в сводке.
     */
    @Query(value = """
            WITH LatestUserActivity AS (
                SELECT user_id, MAX(created_at) AS last_activity
                FROM test_attempts
                GROUP BY user_id
            ),
            -- Курс последней активности пользователя. DISTINCT ON оставляет по
            -- одной строке на user_id, беря самую свежую попытку.
            UserCourse AS (
                SELECT DISTINCT ON (ta.user_id)
                    ta.user_id,
                    m.course_id
                FROM test_attempts ta
                JOIN modules m ON m.id = ta.module_id
                ORDER BY ta.user_id, ta.created_at DESC
            ),
            -- Уроки с тестами по всем курсам сразу: фильтруем их курсом
            -- конкретного пользователя ниже, а не одним общим параметром.
            LessonsWithTests AS (
                SELECT l.id AS lesson_id, m.id AS module_id, m.course_id,
                       m.title AS module_title, m.sequence_order AS module_order
                FROM lessons l
                JOIN modules m ON l.module_id = m.id
                WHERE EXISTS (SELECT 1 FROM tests t WHERE t.lesson_id = l.id)
            ),
            UserTotals AS (
                SELECT
                    uc.user_id,
                    (SELECT COUNT(*)
                     FROM LessonsWithTests lwt
                     WHERE lwt.course_id = uc.course_id) AS total_lessons_with_tests,
                    (SELECT COUNT(DISTINCT t.lesson_id)
                     FROM test_attempts ta
                     JOIN tests t ON t.id = ta.test_id
                     JOIN LessonsWithTests lwt2 ON lwt2.lesson_id = t.lesson_id
                     WHERE ta.user_id = uc.user_id
                       AND ta.passed = true
                       AND lwt2.course_id = uc.course_id) AS total_passed_lessons
                FROM UserCourse uc
            ),
            -- Первый по порядку модуль курса, в котором пройдены не все уроки с
            -- тестами. DISTINCT ON + ORDER BY module_order даёт именно первый.
            NextModule AS (
                SELECT DISTINCT ON (uc.user_id)
                    uc.user_id,
                    lwt.module_title AS next_module_name,
                    lwt.module_order AS next_module_order,
                    (SELECT COUNT(DISTINCT t2.lesson_id)
                     FROM test_attempts ta2
                     JOIN tests t2 ON t2.id = ta2.test_id
                     WHERE ta2.user_id = uc.user_id
                       AND t2.module_id = lwt.module_id
                       AND ta2.passed = true) AS passed_in_next_module
                FROM UserCourse uc
                JOIN LessonsWithTests lwt ON lwt.course_id = uc.course_id
                WHERE (SELECT COUNT(*) FROM LessonsWithTests x WHERE x.module_id = lwt.module_id)
                      > (SELECT COUNT(DISTINCT t3.lesson_id)
                         FROM test_attempts ta3
                         JOIN tests t3 ON t3.id = ta3.test_id
                         WHERE ta3.user_id = uc.user_id
                           AND t3.module_id = lwt.module_id
                           AND ta3.passed = true)
                ORDER BY uc.user_id, lwt.module_order ASC
            )
            SELECT
                u.id                                        AS userId,
                u.username                                  AS fullName,
                COALESCE(ut.total_passed_lessons, 0)        AS totalPassedLessons,
                COALESCE(ut.total_lessons_with_tests, 0)    AS totalLessonsWithTests,
                nm.next_module_name                         AS nextModuleName,
                nm.next_module_order                        AS nextModuleOrder,
                nm.passed_in_next_module                    AS passedInNextModule
            FROM users u
            LEFT JOIN LatestUserActivity lua ON lua.user_id = u.id
            LEFT JOIN UserTotals ut ON ut.user_id = u.id
            LEFT JOIN NextModule nm ON nm.user_id = u.id
            -- NULLS LAST обязателен: в Postgres DESC по умолчанию ставит NULL
            -- первыми, и пользователи без активности вытеснили бы активных.
            ORDER BY lua.last_activity DESC NULLS LAST, u.id ASC
            """, nativeQuery = true)
    List<UserProgressProjection> findUserProgressForDashboard();
}
