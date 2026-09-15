package software.pxel.learneasy.service.impl;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import software.pxel.learneasy.api.dto.admin.AnalyticsSummary;
import software.pxel.learneasy.api.dto.admin.UserGrowthChart;
import software.pxel.learneasy.api.dto.admin.UserTable;
import software.pxel.learneasy.api.dto.admin.projection.AnalyticsSummaryProjection;
import software.pxel.learneasy.api.dto.admin.projection.UserProgressProjection;
import software.pxel.learneasy.api.dto.admin.response.AnalyticResponse;
import software.pxel.learneasy.model.enums.Period;
import software.pxel.learneasy.repository.TestAttemptRepository;
import software.pxel.learneasy.repository.UserRepository;

import java.time.LocalDate;
import java.time.format.TextStyle;
import java.util.List;
import java.util.Locale;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("AnalyticsServiceImpl - сборка дашборда")
class AnalyticsServiceImplTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private TestAttemptRepository testAttemptRepository;

    @InjectMocks
    private AnalyticsServiceImpl analyticsService;

    private AnalyticsSummaryProjection summaryProjection;
    private UserProgressProjection user1Progress;
    private UserProgressProjection user2Progress;

    @BeforeEach
    void setUp() {
        summaryProjection = mock(AnalyticsSummaryProjection.class);
        when(summaryProjection.getTotalUsers()).thenReturn(3000L);
        when(summaryProjection.getNewUsers()).thenReturn(250L);
        when(summaryProjection.getActiveUsers()).thenReturn(500L);
        when(summaryProjection.getRetainedUsers()).thenReturn(425L);
        when(summaryProjection.getTotalOldUsers()).thenReturn(500L);
        when(summaryProjection.getActiveUsersPrevPeriod()).thenReturn(540L);

        user1Progress = mock(UserProgressProjection.class);
        when(user1Progress.getUserId()).thenReturn(123L);
        when(user1Progress.getFullName()).thenReturn("Петров Петр");
        when(user1Progress.getTotalPassedLessons()).thenReturn(7);
        when(user1Progress.getTotalLessonsWithTests()).thenReturn(10);
        when(user1Progress.getNextModuleName()).thenReturn("1. Введение");
        when(user1Progress.getPassedInNextModule()).thenReturn(1);

        user2Progress = mock(UserProgressProjection.class);
        when(user2Progress.getUserId()).thenReturn(456L);
        when(user2Progress.getFullName()).thenReturn("Иванова Мария");
        when(user2Progress.getTotalPassedLessons()).thenReturn(19);
        when(user2Progress.getTotalLessonsWithTests()).thenReturn(20);
        when(user2Progress.getNextModuleName()).thenReturn("3. Продвинутые темы");
        when(user2Progress.getPassedInNextModule()).thenReturn(4);
    }

    @Test
    @DisplayName("Должен корректно собрать все три блока дашборда")
    void getAnalyticsDashboard_shouldAssembleAllBlocksCorrectly() {
        when(userRepository.getAnalyticsSummary("month")).thenReturn(summaryProjection);
        when(testAttemptRepository.findUserProgressForDashboard(anyLong())).thenReturn(List.of(user1Progress, user2Progress));

        AnalyticResponse response = analyticsService.getAnalyticsDashboard(Period.MONTH);

        assertThat(response).isNotNull();

        AnalyticsSummary summary = response.analyticsSummary();
        assertThat(summary).isNotNull();
        assertThat(summary.totalUsers().value()).isEqualTo(3000L);
        assertThat(summary.activeUsers().value()).isEqualTo(500L);
        assertThat(summary.retentionRate().value()).isEqualTo(85);

        List<UserTable> userTable = response.userTable();
        assertThat(userTable).isNotNull();
        assertThat(userTable).hasSize(2);

        UserTable user1 = userTable.get(0);
        assertThat(user1.id()).isEqualTo(123L);
        assertThat(user1.fullName()).isEqualTo("Петров Петр");
        assertThat(user1.progress()).isEqualTo("70%");
        assertThat(user1.currentModule()).isEqualTo("1. Введение");
        assertThat(user1.currentLesson()).isEqualTo(2);

        UserTable user2 = userTable.get(1);
        assertThat(user2.progress()).isEqualTo("95%");
        assertThat(user2.currentModule()).isEqualTo("3. Продвинутые темы");
        assertThat(user2.currentLesson()).isEqualTo(5);

        UserGrowthChart chart = response.userGrowthChart();
        assertThat(chart).isNotNull();
        assertThat(chart.newUsersMonthly()).isEqualTo(250L);
        assertThat(chart.activeUsersMonthly()).isEqualTo(500L);
        assertThat(chart.currentMonthName()).isEqualTo(
                LocalDate.now().getMonth().getDisplayName(TextStyle.FULL, new Locale("ru"))
        );
    }

    @Test
    @DisplayName("В таблицу попадают и активные, и неактивные пользователи")
    void getAnalyticsDashboard_includesUsersWithoutActivity() {
        // После замены JOIN на LEFT JOIN в выборку приходят все пользователи,
        // а не только те, у кого есть пройденные тесты: иначе длина userTable
        // расходилась с totalUsers в сводке.
        UserProgressProjection active = mock(UserProgressProjection.class);
        when(active.getUserId()).thenReturn(1L);
        when(active.getFullName()).thenReturn("Активный");
        when(active.getTotalPassedLessons()).thenReturn(5);
        when(active.getTotalLessonsWithTests()).thenReturn(10);

        UserProgressProjection idle = mock(UserProgressProjection.class);
        when(idle.getUserId()).thenReturn(2L);
        when(idle.getFullName()).thenReturn("Без активности");
        when(idle.getTotalPassedLessons()).thenReturn(null);
        when(idle.getTotalLessonsWithTests()).thenReturn(null);

        when(userRepository.getAnalyticsSummary("month")).thenReturn(summaryProjection);
        when(testAttemptRepository.findUserProgressForDashboard(anyLong()))
                .thenReturn(List.of(active, idle));

        AnalyticResponse response = analyticsService.getAnalyticsDashboard(Period.MONTH);

        assertThat(response.userTable()).hasSize(2);
        assertThat(response.userTable().get(0).progress()).isEqualTo("50%");
        assertThat(response.userTable().get(1).progress()).isEqualTo("0%");
    }

    @Test
    @DisplayName("Пользователь без прогресса по курсу не роняет дашборд")
    void getAnalyticsDashboard_userWithoutProgress_doesNotFail() {
        // Реальный случай с прода: пользователь попадает в выборку по активности
        // (JOIN LatestUserActivity), но строк прогресса по курсу у него нет, и
        // LEFT JOIN отдаёт null. Распаковка null в int роняла весь дашборд.
        UserProgressProjection noProgress = mock(UserProgressProjection.class);
        when(noProgress.getUserId()).thenReturn(789L);
        when(noProgress.getFullName()).thenReturn("Новичок Без Прогресса");
        when(noProgress.getTotalPassedLessons()).thenReturn(null);
        when(noProgress.getTotalLessonsWithTests()).thenReturn(null);
        when(noProgress.getNextModuleName()).thenReturn(null);
        when(noProgress.getPassedInNextModule()).thenReturn(null);

        when(userRepository.getAnalyticsSummary("month")).thenReturn(summaryProjection);
        when(testAttemptRepository.findUserProgressForDashboard(anyLong())).thenReturn(List.of(noProgress));

        AnalyticResponse response = analyticsService.getAnalyticsDashboard(Period.MONTH);

        UserTable row = response.userTable().get(0);
        assertThat(row.id()).isEqualTo(789L);
        assertThat(row.progress()).isEqualTo("0%");
        assertThat(row.currentLesson()).isEqualTo(1);
        assertThat(row.currentModule()).isNull();
    }
}
