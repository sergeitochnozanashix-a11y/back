package software.pxel.learneasy.service;

import software.pxel.learneasy.api.dto.question.QuestionRequest;
import software.pxel.learneasy.model.TestModel;

import java.util.List;

public interface QuestionService {
    void replaceQuestionsForTest(TestModel testModel, List<QuestionRequest> questionRequests);
}
