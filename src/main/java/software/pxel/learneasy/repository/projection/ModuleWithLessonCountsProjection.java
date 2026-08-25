package software.pxel.learneasy.repository.projection;

public interface ModuleWithLessonCountsProjection {
    Long getModuleId();

    String getTitle();

    String getDescription();

    Long getLessonCount();

    Integer getSequenceOrder();
}
