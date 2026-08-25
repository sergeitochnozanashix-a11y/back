package software.pxel.learneasy.service.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;
import software.pxel.learneasy.api.dto.admin.AnalyticsSummary;
import software.pxel.learneasy.api.dto.admin.UserGrowthChart;
import software.pxel.learneasy.api.dto.admin.UserTable;
import software.pxel.learneasy.api.dto.admin.projection.AnalyticsSummaryProjection;
import software.pxel.learneasy.api.dto.admin.projection.UserProgressProjection;
import software.pxel.learneasy.api.dto.admin.response.AnalyticResponse;
import software.pxel.learneasy.model.enums.Period;
import software.pxel.learneasy.repository.TestAttemptRepository;
import software.pxel.learneasy.repository.UserRepository;
import software.pxel.learneasy.service.AnalyticsService;

import java.time.LocalDate;
import java.time.format.TextStyle;
import java.util.List;
import java.util.Locale;

@Slf4j
@Service
@RequiredArgsConstructor
public class AnalyticsServiceImpl implements AnalyticsService {

    private static final Long DEFAULT_COURSE_ID = 1L;
    public static final String LOCALE_RU = "ru";

    private final UserRepository userRepository;
    private final TestAttemptRepository testAttemptRepository;

    @Override
    @Transactional(readOnly = true, isolation = Isolation.REPEATABLE_READ)
    public AnalyticResponse getAnalyticsDashboard(Period period) {
        AnalyticsSummaryProjection summaryProjection = userRepository.getAnalyticsSummary(period.toString());

        AnalyticsSummary summary = buildAnalyticsSummary(summaryProjection);
        UserGrowthChart chart = buildUserGrowthChart(summaryProjection);

        List<UserTable> userTable = buildUserTable();

        return new AnalyticResponse(summary, userTable, chart);
    }

    private AnalyticsSummary buildAnalyticsSummary(AnalyticsSummaryProjection proj) {
        AnalyticsSummary.TotalUsers totalUsers = new AnalyticsSummary.TotalUsers(
                proj.getTotalUsers(),
                calculateChange(proj.getTotalUsers(), proj.getTotalUsersPrevPeriod())
        );
        AnalyticsSummary.ActiveUsers activeUsers = new AnalyticsSummary.ActiveUsers(
                Math.toIntExact(proj.getActiveUsers()),
                calculateChange(proj.getActiveUsers(), proj.getActiveUsersPrevPeriod())
        );
        AnalyticsSummary.RetentionRate retentionRate = new AnalyticsSummary.RetentionRate(
                calculatePercentage(proj.getRetainedUsers(), proj.getTotalOldUsers()),
                calculateChange(
                        calculatePercentage(proj.getRetainedUsers(), proj.getTotalOldUsers()),
                        calculatePercentage(proj.getRetainedUsersPrevPeriod(), proj.getTotalOldUsersPrevPeriod())
                )
        );

        return new AnalyticsSummary(totalUsers, activeUsers, retentionRate);
    }

    private UserGrowthChart buildUserGrowthChart(AnalyticsSummaryProjection proj) {
        return new UserGrowthChart(
                LocalDate.now().getMonth().getDisplayName(TextStyle.FULL, new Locale(LOCALE_RU)),
                Math.toIntExact(proj.getNewUsers()),
                proj.getActiveUsers()
        );
    }

    private List<UserTable> buildUserTable() {
        List<UserProgressProjection> projections = testAttemptRepository.findUserProgressForDashboard(DEFAULT_COURSE_ID);

        return projections.stream().map(proj -> {
            int progress = calculatePercentage(proj.getTotalPassedLessons(), proj.getTotalLessonsWithTests());
            return new UserTable(
                    proj.getUserId(),
                    proj.getFullName(),
                    progress + "%",
                    proj.getNextModuleName(),
                    (proj.getPassedInNextModule() != null ? proj.getPassedInNextModule() : 0) + 1
            );
        }).toList();
    }

    private float calculateChange(long current, long previous) {
        if (previous == 0) return current > 0 ? 100.0f : 0.0f;
        return ((float) (current - previous) / previous) * 100.0f;
    }

    private int calculatePercentage(long part, long total) {
        if (total == 0) return 0;
        return (int) (((double) part / total) * 100);
    }
}
