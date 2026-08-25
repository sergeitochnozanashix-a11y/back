package software.pxel.learneasy.service.impl;

import jakarta.persistence.EntityNotFoundException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import software.pxel.learneasy.api.dto.mainpage.UserActivityStatsResponse;
import software.pxel.learneasy.api.dto.userprogress.CourseProgressResponse;
import software.pxel.learneasy.model.Course;
import software.pxel.learneasy.model.Lesson;
import software.pxel.learneasy.model.Module;
import software.pxel.learneasy.model.User;
import software.pxel.learneasy.repository.*;

import java.time.Instant;
import java.time.LocalDate;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("UserProgressService — прогресс пользователя")
class UserProgressServiceImplTest {

    @Mock
    CourseRepository courseRepository;
    @Mock
    ModuleRepository moduleRepository;
    @Mock
    UserRepository userRepository;
    @Mock
    TestAttemptRepository testAttemptRepository;
    @Mock
    TestModelRepository testModelRepository;

    @InjectMocks
    UserProgressServiceImpl service;

    @Nested
    @DisplayName("getCourseProgress(userId, courseId) — агрегированный прогресс по курсу")
    class GetCourseProgress {

        @Test
        @DisplayName("404 — пользователь не найден")
        void userNotFound() {
            when(userRepository.findById(1L)).thenReturn(Optional.empty());

            assertThrows(EntityNotFoundException.class, () -> service.getCourseProgress(1L, 100L));

            verify(userRepository).findById(1L);
            verifyNoInteractions(courseRepository, moduleRepository, testAttemptRepository, testModelRepository);
        }

        @Test
        @DisplayName("404 — курс не найден")
        void courseNotFound() {
            when(userRepository.findById(1L)).thenReturn(Optional.of(user(1L, "u")));
            when(courseRepository.findById(100L)).thenReturn(Optional.empty());

            assertThrows(EntityNotFoundException.class, () -> service.getCourseProgress(1L, 100L));

            verify(userRepository).findById(1L);
            verify(courseRepository).findById(100L);
            verifyNoInteractions(moduleRepository, testAttemptRepository, testModelRepository);
        }

        @Test
        @DisplayName("успех — корректно агрегирует модули, уроки и статусы завершённости")
        void success_aggregatesTotalsAndModuleStatuses() {
            long userId = 2L, courseId = 200L;

            when(userRepository.findById(userId)).thenReturn(Optional.of(user(userId, "u")));
            when(courseRepository.findById(courseId)).thenReturn(Optional.of(course(courseId, "C")));

            Module m1 = module(11L, "M1", course(courseId, "C")); // 2 урока, 1 с тестом (пройден) -> COMPLETED
            Module m2 = module(12L, "M2", course(courseId, "C")); // 1 урок, 1 с тестом (не пройден) -> NOT COMPLETED
            Module m3 = module(13L, "M3", course(courseId, "C")); // 1 урок, нет тестов -> COMPLETED
            lesson(101L, m1);
            lesson(102L, m1);
            lesson(201L, m2);
            lesson(301L, m3);

            when(moduleRepository.findAllByCourseId(courseId)).thenReturn(List.of(m1, m2, m3));

            // Мокируем уроки, у которых есть тесты
            List<TestModelRepository.ModuleLessonPair> pairs = List.of(
                    moduleLessonPair(11L, 101L), // M1 -> L101
                    moduleLessonPair(12L, 201L)  // M2 -> L201
            );
            when(testModelRepository.findModuleLessonPairsWithTestsByCourseId(courseId)).thenReturn(pairs);

            // Мокируем пройденные тесты
            when(testAttemptRepository.findPassedLessonIdsByUserIdAndCourseId(userId, courseId)).thenReturn(Set.of(101L));

            // Мокируем общее количество пройденных уроков
            when(testAttemptRepository.countDistinctPassedLessonsByUserIdAndCourseId(userId, courseId)).thenReturn(1L);

            // Вызов
            CourseProgressResponse resp = service.getCourseProgress(userId, courseId);

            // Проверка
            assertEquals(3L, resp.totalModules());
            assertEquals(4L, resp.totalLessons());
            assertEquals(1L, resp.completedLessons());
            assertEquals(2L, resp.completedModules(), "Модули 1 и 3 должны быть завершены");

            assertEquals(3, resp.modules().size());
            assertTrue(resp.modules().stream().anyMatch(p -> p.moduleTitle().equals("M1") && p.isCompleted()));
            assertTrue(resp.modules().stream().anyMatch(p -> p.moduleTitle().equals("M2") && !p.isCompleted()));
            assertTrue(resp.modules().stream().anyMatch(p -> p.moduleTitle().equals("M3") && p.isCompleted()));
        }
    }

    @Nested
    @DisplayName("getUserActivityStats(userId, start, end) — дневная активность за период")
    class GetUserActivityStats {

        @Test
        @DisplayName("успех — заполняет все дни периода и считает только тесты")
        void success_fillsAllDaysAndCountsOnlyTests() {
            long userId = 5L;
            LocalDate start = LocalDate.of(2024, 7, 1);
            LocalDate end = LocalDate.of(2024, 7, 3);

            // Тесты: 2 июля
            List<Object[]> tests = new ArrayList<>();
            tests.add(new Object[]{java.sql.Date.valueOf(LocalDate.of(2024, 7, 2)), 3L});
            when(testAttemptRepository.findDailyTestStats(eq(userId), any(Instant.class), any(Instant.class)))
                    .thenReturn(tests);

            // Вызов
            List<UserActivityStatsResponse> out = service.getUserActivityStats(userId, start, end);

            // Проверка
            assertEquals(3, out.size());
            // 1 июля
            assertEquals(LocalDate.of(2024, 7, 1), out.get(0).date());
            assertEquals(0L, out.get(0).lessonsCompleted());
            assertEquals(0L, out.get(0).testsPassed());
            // 2 июля
            assertEquals(LocalDate.of(2024, 7, 2), out.get(1).date());
            assertEquals(0L, out.get(1).lessonsCompleted());
            assertEquals(3L, out.get(1).testsPassed());
            // 3 июля
            assertEquals(LocalDate.of(2024, 7, 3), out.get(2).date());
            assertEquals(0L, out.get(2).lessonsCompleted());
            assertEquals(0L, out.get(2).testsPassed());

            verify(testAttemptRepository).findDailyTestStats(eq(userId), any(Instant.class), any(Instant.class));
        }
    }

    // Хелперы
    private static User user(Long id, String username) {
        User u = new User();
        u.setId(id);
        u.setUsername(username);
        return u;
    }

    private static Course course(Long id, String title) {
        Course c = new Course();
        c.setId(id);
        c.setTitle(title);
        return c;
    }

    private static Module module(Long id, String title, Course course) {
        Module m = new Module();
        m.setId(id);
        m.setTitle(title);
        m.setCourse(course);
        m.setLessons(new ArrayList<>());
        return m;
    }

    private static void lesson(Long id, Module module) {
        Lesson l = new Lesson();
        l.setId(id);
        l.setModule(module);
        module.getLessons().add(l);
    }

    private static TestModelRepository.ModuleLessonPair moduleLessonPair(Long moduleId, Long lessonId) {
        return new TestModelRepository.ModuleLessonPair() {
            @Override
            public Long getModuleId() {
                return moduleId;
            }

            @Override
            public Long getLessonId() {
                return lessonId;
            }
        };
    }
}
