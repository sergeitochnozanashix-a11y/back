package software.pxel.learneasy.service.impl;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import software.pxel.learneasy.api.dto.mainpage.MainPageInfoResponse;
import software.pxel.learneasy.api.dto.mainpage.UserActivityStatsResponse;
import software.pxel.learneasy.api.dto.module.ModuleWithProgressDTO;
import software.pxel.learneasy.api.dto.userprogress.CourseProgressResponse;
import software.pxel.learneasy.model.Course;
import software.pxel.learneasy.model.enums.CompletionStatus;
import org.springframework.data.domain.Pageable;
import software.pxel.learneasy.repository.CourseRepository;
import software.pxel.learneasy.repository.TestAttemptRepository;
import software.pxel.learneasy.service.UserProgressService;

import java.time.LocalDate;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("MainPageService — агрегированная инфа для главной страницы")
class MainPageServiceImplTest {

    @Mock
    private UserProgressService userProgressService;
    @Mock
    private CourseRepository courseRepository;
    @Mock
    private TestAttemptRepository testAttemptRepository;

    @InjectMocks
    private MainPageServiceImpl mainPageService;

    private static final long TEST_USER_ID = 1L;
    // Намеренно не 1: курс больше не захардкожен, он приходит из активности.
    private static final long ACTIVE_COURSE_ID = 42L;

    @Test
    @DisplayName("getMainPageInfo — частичное прохождение: корректные поля, currentModuleId — первый незавершённый")
    void getMainPageInfo_partialProgress() {
        Course mockCourse = createMockCourse();
        CourseProgressResponse mockProgress = createMockCourseProgress();
        List<ModuleWithProgressDTO> mockModules = createMockModulesWithPartialProgress();
        List<UserActivityStatsResponse> weekly = createWeeklyActivity();

        when(testAttemptRepository.findLastActiveCourseIds(eq(TEST_USER_ID), any(Pageable.class)))
                .thenReturn(List.of(ACTIVE_COURSE_ID));
        when(courseRepository.findById(ACTIVE_COURSE_ID)).thenReturn(Optional.of(mockCourse));
        when(userProgressService.getCourseProgress(TEST_USER_ID, ACTIVE_COURSE_ID)).thenReturn(mockProgress);
        when(userProgressService.getModulesWithProgress(ACTIVE_COURSE_ID, TEST_USER_ID)).thenReturn(mockModules);
        when(userProgressService.getUserActivityStats(eq(TEST_USER_ID), any(LocalDate.class), any(LocalDate.class)))
                .thenReturn(weekly);

        MainPageInfoResponse response = mainPageService.getMainPageInfo(TEST_USER_ID);

        assertNotNull(response);
        assertNotNull(response.courseInfo());
        assertNotNull(response.modules());
        assertNotNull(response.weeklyActivity());

        assertEquals(ACTIVE_COURSE_ID, response.courseInfo().id());
        assertEquals(mockCourse.getTitle(), response.courseInfo().title());
        assertEquals(10L, response.courseInfo().totalModules());
        assertEquals(50L, response.courseInfo().totalLessons());
        assertEquals(1L, response.courseInfo().completedModules());
        assertEquals(8L, response.courseInfo().completedLessons());
        assertEquals(20L, response.courseInfo().currentModuleId());

        assertEquals(3, response.modules().size());
        assertEquals(10L, response.modules().get(0).id());
        assertEquals(CompletionStatus.COMPLETED.toString(), response.modules().get(0).completionStatus());
        // Поля контракта: в спеке они значились, но в ответ не попадали -
        // sequenceOrder приходил под именем sequenceNumber, description терялся.
        assertEquals(1, response.modules().get(0).sequenceOrder());
        assertEquals("Описание 1", response.modules().get(0).description());
        assertEquals(20L, response.modules().get(1).id());
        assertEquals(CompletionStatus.IN_PROGRESS.toString(), response.modules().get(1).completionStatus());
        assertEquals(30L, response.modules().get(2).id());
        assertEquals(CompletionStatus.BLOCKED.toString(), response.modules().get(2).completionStatus());

        assertEquals(7, response.weeklyActivity().size());

        verify(courseRepository).findById(ACTIVE_COURSE_ID);
        verify(userProgressService).getCourseProgress(TEST_USER_ID, ACTIVE_COURSE_ID);
        verify(userProgressService).getModulesWithProgress(ACTIVE_COURSE_ID, TEST_USER_ID);
        verify(userProgressService).getUserActivityStats(eq(TEST_USER_ID), any(LocalDate.class), any(LocalDate.class));
        verifyNoMoreInteractions(courseRepository, userProgressService);
    }

    @Test
    @DisplayName("getMainPageInfo — все модули пройдены: currentModuleId == null")
    void getMainPageInfo_allCompleted_currentModuleNull() {
        Course mockCourse = createMockCourse();
        CourseProgressResponse mockProgress = createMockCourseProgress();
        List<ModuleWithProgressDTO> mockModules = createMockModulesWithFullProgress();
        List<UserActivityStatsResponse> weekly = createWeeklyActivity();

        when(testAttemptRepository.findLastActiveCourseIds(eq(TEST_USER_ID), any(Pageable.class)))
                .thenReturn(List.of(ACTIVE_COURSE_ID));
        when(courseRepository.findById(ACTIVE_COURSE_ID)).thenReturn(Optional.of(mockCourse));
        when(userProgressService.getCourseProgress(TEST_USER_ID, ACTIVE_COURSE_ID)).thenReturn(mockProgress);
        when(userProgressService.getModulesWithProgress(ACTIVE_COURSE_ID, TEST_USER_ID)).thenReturn(mockModules);
        when(userProgressService.getUserActivityStats(eq(TEST_USER_ID), any(LocalDate.class), any(LocalDate.class)))
                .thenReturn(weekly);

        MainPageInfoResponse response = mainPageService.getMainPageInfo(TEST_USER_ID);

        assertNotNull(response);
        assertNotNull(response.courseInfo());
        assertNull(response.courseInfo().currentModuleId());
        assertEquals(7, response.weeklyActivity().size());
    }

    @Test
    @DisplayName("getMainPageInfo — прогресса нет: currentModuleId — первый модуль")
    void getMainPageInfo_noProgress_firstModule() {
        Course mockCourse = createMockCourse();
        CourseProgressResponse mockProgress = new CourseProgressResponse(10L, 0L, 50L, 0L, Collections.emptyList());
        List<ModuleWithProgressDTO> mockModules = createMockModulesWithNoProgress();
        List<UserActivityStatsResponse> weekly = createWeeklyActivity();

        when(testAttemptRepository.findLastActiveCourseIds(eq(TEST_USER_ID), any(Pageable.class)))
                .thenReturn(List.of(ACTIVE_COURSE_ID));
        when(courseRepository.findById(ACTIVE_COURSE_ID)).thenReturn(Optional.of(mockCourse));
        when(userProgressService.getCourseProgress(TEST_USER_ID, ACTIVE_COURSE_ID)).thenReturn(mockProgress);
        when(userProgressService.getModulesWithProgress(ACTIVE_COURSE_ID, TEST_USER_ID)).thenReturn(mockModules);
        when(userProgressService.getUserActivityStats(eq(TEST_USER_ID), any(LocalDate.class), any(LocalDate.class)))
                .thenReturn(weekly);

        MainPageInfoResponse response = mainPageService.getMainPageInfo(TEST_USER_ID);

        assertNotNull(response);
        assertEquals(10L, response.courseInfo().currentModuleId());
        assertEquals(0L, response.courseInfo().completedLessons());
        assertEquals(0L, response.courseInfo().completedModules());
        assertEquals(7, response.weeklyActivity().size());
    }

    @Test
    @DisplayName("getMainPageInfo — нет активности → 200 с пустым ответом, а не 404")
    void getMainPageInfo_noActivity_returnsEmptyResponse() {
        when(testAttemptRepository.findLastActiveCourseIds(eq(TEST_USER_ID), any(Pageable.class)))
                .thenReturn(List.of());

        MainPageInfoResponse response = mainPageService.getMainPageInfo(TEST_USER_ID);

        assertNotNull(response);
        assertNull(response.courseInfo());
        assertTrue(response.modules().isEmpty());
        assertTrue(response.weeklyActivity().isEmpty());
        // Ни курс, ни прогресс не запрашиваются: спрашивать нечего.
        verifyNoInteractions(userProgressService);
        verifyNoInteractions(courseRepository);
    }

    @Test
    @DisplayName("getMainPageInfo — курс из активности удалён → 200 с пустым ответом")
    void getMainPageInfo_courseDeleted_returnsEmptyResponse() {
        when(testAttemptRepository.findLastActiveCourseIds(eq(TEST_USER_ID), any(Pageable.class)))
                .thenReturn(List.of(ACTIVE_COURSE_ID));
        when(courseRepository.findById(ACTIVE_COURSE_ID)).thenReturn(Optional.empty());

        MainPageInfoResponse response = mainPageService.getMainPageInfo(TEST_USER_ID);

        assertNull(response.courseInfo());
        verifyNoInteractions(userProgressService);
    }

    @Test
    @DisplayName("getMainPageInfo — берётся курс последней активности, а не id=1")
    void getMainPageInfo_usesLastActiveCourse() {
        Course mockCourse = createMockCourse();
        when(testAttemptRepository.findLastActiveCourseIds(eq(TEST_USER_ID), any(Pageable.class)))
                .thenReturn(List.of(ACTIVE_COURSE_ID));
        when(courseRepository.findById(ACTIVE_COURSE_ID)).thenReturn(Optional.of(mockCourse));
        when(userProgressService.getCourseProgress(TEST_USER_ID, ACTIVE_COURSE_ID))
                .thenReturn(createMockCourseProgress());
        when(userProgressService.getModulesWithProgress(ACTIVE_COURSE_ID, TEST_USER_ID))
                .thenReturn(createMockModulesWithPartialProgress());
        when(userProgressService.getUserActivityStats(eq(TEST_USER_ID), any(LocalDate.class), any(LocalDate.class)))
                .thenReturn(createWeeklyActivity());

        MainPageInfoResponse response = mainPageService.getMainPageInfo(TEST_USER_ID);

        assertEquals(ACTIVE_COURSE_ID, response.courseInfo().id());
        verify(courseRepository).findById(ACTIVE_COURSE_ID);
        verify(courseRepository, never()).findById(1L);
    }

    // ------------------------------------------------------------
    // Helpers

    private Course createMockCourse() {
        Course course = new Course();
        course.setId(ACTIVE_COURSE_ID);
        course.setTitle("Основы Java-разработки");
        course.setDescription("Курс для начинающих.");
        return course;
    }

    private CourseProgressResponse createMockCourseProgress() {
        return new CourseProgressResponse(10L, 1L, 50L, 8L, Collections.emptyList());
    }

    private List<ModuleWithProgressDTO> createMockModulesWithPartialProgress() {
        return List.of(
                new ModuleWithProgressDTO(10L, 1, "Модуль 1", "Описание 1", 5, 5, CompletionStatus.COMPLETED.toString()),
                new ModuleWithProgressDTO(20L, 2, "Модуль 2", "Описание 2", 5, 3, CompletionStatus.IN_PROGRESS.toString()),
                new ModuleWithProgressDTO(30L, 3, "Модуль 3", "Описание 3", 5, 0, CompletionStatus.BLOCKED.toString())
        );
    }

    private List<ModuleWithProgressDTO> createMockModulesWithFullProgress() {
        return List.of(
                new ModuleWithProgressDTO(10L, 1, "Модуль 1", "Описание 1", 5, 5, CompletionStatus.COMPLETED.toString()),
                new ModuleWithProgressDTO(20L, 2, "Модуль 2", "Описание 2", 5, 5, CompletionStatus.COMPLETED.toString())
        );
    }

    private List<ModuleWithProgressDTO> createMockModulesWithNoProgress() {
        return List.of(
                new ModuleWithProgressDTO(10L, 1, "Модуль 1", "Описание 1", 5, 0, CompletionStatus.NOT_STARTED.toString()),
                new ModuleWithProgressDTO(20L, 2, "Модуль 2", "Описание 2", 5, 0, CompletionStatus.BLOCKED.toString())
        );
    }

    private List<UserActivityStatsResponse> createWeeklyActivity() {
        LocalDate today = LocalDate.now();
        return List.of(
                new UserActivityStatsResponse(today.minusDays(6), 0L, 0L),
                new UserActivityStatsResponse(today.minusDays(5), 0L, 0L),
                new UserActivityStatsResponse(today.minusDays(4), 0L, 1L),
                new UserActivityStatsResponse(today.minusDays(3), 0L, 0L),
                new UserActivityStatsResponse(today.minusDays(2), 0L, 0L),
                new UserActivityStatsResponse(today.minusDays(1), 0L, 1L),
                new UserActivityStatsResponse(today, 0L, 0L)
        );
    }
}
