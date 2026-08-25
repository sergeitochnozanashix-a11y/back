package software.pxel.learneasy.mapper;

import org.mapstruct.IterableMapping;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.ReportingPolicy;
import software.pxel.learneasy.api.dto.attempt.AnswerView;
import software.pxel.learneasy.api.dto.attempt.TestAttemptResponse;
import software.pxel.learneasy.model.TestAnswer;
import software.pxel.learneasy.model.TestAttempt;

import java.util.List;

@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.ERROR)
public interface TestAttemptMapper {

    @Mapping(target = "id", source = "attempt.id")
    @Mapping(target = "userId", source = "attempt.userId")
    @Mapping(target = "testId", source = "attempt.test.id")
    @Mapping(target = "lessonId", source = "attempt.lesson.id")
    @Mapping(target = "moduleId", source = "attempt.module.id")
    @Mapping(target = "status", source = "attempt.status")
    @Mapping(target = "answers", source = "answers")
    @Mapping(target = "scorePercent", source = "attempt.scorePercent")
    @Mapping(target = "passed", source = "attempt.passed")
    @Mapping(target = "createdAt", source = "attempt.createdAt")
    @Mapping(target = "updatedAt", source = "attempt.updatedAt")
    @Mapping(target = "evaluatedAt", source = "attempt.evaluatedAt")
    @Mapping(target = "failedReason", source = "attempt.failedReason")
    TestAttemptResponse toResponse(TestAttempt attempt, List<TestAnswer> answers);

    @Mapping(target = "questionId", source = "question.id")
    @Mapping(target = "questionText", source = "question.text")
    @Mapping(target = "inputType", source = "inputType")
    @Mapping(target = "textAnswer", source = "textAnswer")
    @Mapping(target = "voiceFileUrl", source = "voiceFileUrl")
    @Mapping(target = "voiceTranscript", source = "voiceTranscript")
    @Mapping(target = "evaluation", source = "evaluation")
    @Mapping(target = "notes", source = "evaluationNotes")
    AnswerView toAnswerView(TestAnswer answer);

    @IterableMapping(elementTargetType = AnswerView.class)
    List<AnswerView> toAnswerViews(List<TestAnswer> answers);
}
