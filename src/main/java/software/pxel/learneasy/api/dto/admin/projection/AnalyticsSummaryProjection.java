package software.pxel.learneasy.api.dto.admin.projection;

public interface AnalyticsSummaryProjection {
    Long getTotalUsers();

    Long getNewUsers();

    Long getActiveUsers();

    Long getRetainedUsers();

    Long getTotalOldUsers();

    Long getTotalUsersPrevPeriod();

    Long getActiveUsersPrevPeriod();

    Long getRetainedUsersPrevPeriod();

    Long getTotalOldUsersPrevPeriod();
}
