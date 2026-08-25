package software.pxel.learneasy.repository.projection;

public interface TestModelWithQuestionsCount {
    Long getId();

    String getTitle();

    Long getModuleId();

    String getModuleTitle();

    Integer getModuleSequenceOrder();

    Integer getQuestionsCount();

    Integer getPassThresholdPercentage();
}
