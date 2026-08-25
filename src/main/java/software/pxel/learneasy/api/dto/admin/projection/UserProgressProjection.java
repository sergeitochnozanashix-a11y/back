package software.pxel.learneasy.api.dto.admin.projection;

public interface UserProgressProjection {

    Long getUserId();

    String getFullName();

    Integer getTotalPassedLessons();

    Integer getTotalLessonsWithTests();

    String getNextModuleName();

    Integer getNextModuleOrder();

    Integer getPassedInNextModule();
}
