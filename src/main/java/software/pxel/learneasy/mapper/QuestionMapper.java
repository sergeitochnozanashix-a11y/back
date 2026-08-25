package software.pxel.learneasy.mapper;

import org.mapstruct.Builder;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import software.pxel.learneasy.api.dto.question.QuestionRequest;
import software.pxel.learneasy.api.dto.question.QuestionResponse;
import software.pxel.learneasy.model.Question;

@Mapper(componentModel = "spring", builder = @Builder(disableBuilder = true))
public interface QuestionMapper {

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "test", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    Question toQuestion(QuestionRequest questionRequest);

    @Mapping(target = "questionId", source = "id")
    QuestionResponse toQuestionResponse(Question question);
}
