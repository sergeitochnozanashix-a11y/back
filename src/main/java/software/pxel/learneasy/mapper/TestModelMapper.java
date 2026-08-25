package software.pxel.learneasy.mapper;

import org.mapstruct.Builder;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;
import software.pxel.learneasy.api.dto.test.TestRequest;
import software.pxel.learneasy.api.dto.test.TestResponse;
import software.pxel.learneasy.model.Lesson;
import software.pxel.learneasy.model.Module;
import software.pxel.learneasy.model.TestModel;

@Mapper(componentModel = "spring", uses = QuestionMapper.class, builder = @Builder(disableBuilder = true))
public interface TestModelMapper {

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "lesson", source = "lesson")
    @Mapping(target = "module", source = "module")
    @Mapping(target = "title", source = "testRequest.title")
    @Mapping(target = "questions", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    TestModel toLessonTestModel(TestRequest testRequest, Lesson lesson, Module module);

    @Mapping(target = "lessonId", source = "lesson.id")
    @Mapping(target = "moduleId", source = "module.id")
    @Mapping(target = "lessonSequenceOrder", source = "lesson.sequenceOrder")
    @Mapping(target = "moduleSequenceOrder", source = "module.sequenceOrder")
    TestResponse toTestResponse(TestModel testModel);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "lesson", ignore = true)
    @Mapping(target = "questions", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    void updateTestFromRequest(TestRequest testRequest, @MappingTarget TestModel testModel);
}
