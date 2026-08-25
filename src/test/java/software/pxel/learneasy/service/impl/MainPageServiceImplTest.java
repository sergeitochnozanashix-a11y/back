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
import software.pxel.learneasy.exception.ResourceNotFoundException;
import software.pxel.learneasy.model.Course;
import software.pxel.learneasy.model.enums.CompletionStatus;
import software.pxel.learneasy.repository.CourseRepository;
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

    @InjectMocks
    private MainPageServiceImpl mainPageService;

    private static final long TEST_USER_ID = 1L;
    private static final long DEFAULT_COURSE_ID = 1L;

    @Test
    @DisplayName("getMainPageInfo — частичное прохождение: корректные поля, currentModuleId — первый незавершённый")
    void getMainPageInfo_partialProgress() {
        Course mockCourse = createMockCourse();
        CourseProgressResponse mockProgress = createMockCourseProgress();
        List<ModuleWithProgressDTO> mockModules = createMockModulesWithPartialProgress();
        List<UserActivityStatsResponse> weekly = createWeeklyActivity();

        when(courseRepository.findById(DEFAULT_COURSE_ID)).thenReturn(Optional.of(mockCourse));
        when(userProgressService.getCourseProgress(TEST_USER_ID, DEFAULT_COURSE_ID)).thenReturn(mockProgress);
        when(userProgressService.getModulesWithProgress(DEFAULT_COURSE_ID, TEST_USER_ID)).thenReturn(mockModules);
        when(userProgressService.getUserActivityStats(eq(TEST_USER_ID), any(LocalDate.class), any(LocalDate.class)))
                .thenReturn(weekly);

        MainPageInfoResponse response = mainPageService.getMainPageInfo(TEST_USER_ID);

        assertNotNull(response);
        assertNotNull(response.courseInfo());
        assertNotNull(response.modules());
        assertNotNull(response.weeklyActivity());

        assertEquals(DEFAULT_COURSE_ID, response.courseInfo().id());
        assertEquals(mockCourse.getTitle(), response.courseInfo().title());
        assertEquals(10L, response.courseInfo().totalModules());
        assertEquals(50L, response.courseInfo().totalLessons());
        assertEquals(1L, response.courseInfo().completedModules());
        assertEquals(8L, response.courseInfo().completedLessons());
        assertEquals(20L, response.courseInfo().currentModuleId());

        assertEquals(3, response.modules().size());
        assertEquals(10L, response.modules().get(0).id());
        assertEquals(CompletionStatus.COMPLETED.toString(), response.modules().get(0).completionStatus());
        assertEquals(20L, response.modules().get(1).id());
        assertEquals(CompletionStatus.IN_PROGRESS.toString(), response.modules().get(1).completionStatus());
        assertEquals(30L, response.modules().get(2).id());
        assertEquals(CompletionStatus.BLOCKED.toString(), response.modules().get(2).completionStatus());

        assertEquals(7, response.weeklyActivity().size());

        verify(courseRepository).findById(DEFAULT_COURSE_ID);
        verify(userProgressService).getCourseProgress(TEST_USER_ID, DEFAULT_COURSE_ID);
        verify(userProgressService).getModulesWithProgress(DEFAULT_COURSE_ID, TEST_USER_ID);
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

        when(courseRepository.findById(DEFAULT_COURSE_ID)).thenReturn(Optional.of(mockCourse));
        when(userProgressService.getCourseProgress(TEST_USER_ID, DEFAULT_COURSE_ID)).thenReturn(mockProgress);
        when(userProgressService.getModulesWithProgress(DEFAULT_COURSE_ID, TEST_USER_ID)).thenReturn(mockModules);
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

        when(courseRepository.findById(DEFAULT_COURSE_ID)).thenReturn(Optional.of(mockCourse));
        when(userProgressService.getCourseProgress(TEST_USER_ID, DEFAULT_COURSE_ID)).thenReturn(mockProgress);
        when(userProgressService.getModulesWithProgress(DEFAULT_COURSE_ID, TEST_USER_ID)).thenReturn(mockModules);
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
    @DisplayName("getMainPageInfo — курс по умолчанию не найден → 404")
    void getMainPageInfo_defaultCourseNotFound() {
        when(courseRepository.findById(DEFAULT_COURSE_ID)).thenReturn(Optional.empty());

        ResourceNotFoundException ex = assertThrows(ResourceNotFoundException.class,
                () -> mainPageService.getMainPageInfo(TEST_USER_ID));

        assertTrue(ex.getMessage().contains("Default course with ID " + DEFAULT_COURSE_ID + " not found."));
        verify(courseRepository).findById(DEFAULT_COURSE_ID);
        verifyNoInteractions(userProgressService);
    }

    // ------------------------------------------------------------
    // Helpers

    private Course createMockCourse() {
        Course course = new Course();
        course.setId(DEFAULT_COURSE_ID);
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
