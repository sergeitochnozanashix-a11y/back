package software.pxel.learneasy.service.impl;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import software.pxel.learneasy.api.dto.question.QuestionRequest;
import software.pxel.learneasy.mapper.QuestionMapper;
import software.pxel.learneasy.model.Question;
import software.pxel.learneasy.model.TestModel;
import software.pxel.learneasy.repository.QuestionRepository;
import software.pxel.learneasy.service.QuestionService;

import java.util.List;

@Service
@RequiredArgsConstructor
public class QuestionServiceImpl implements QuestionService {

    private final QuestionRepository questionRepository;
    private final QuestionMapper questionMapper;

    @Override
    @Transactional(propagation = Propagation.MANDATORY)
    public void replaceQuestionsForTest(TestModel testModel, List<QuestionRequest> questionRequests) {
        if (testModel.getId() != null) {
            questionRepository.deleteAllByTestId(testModel.getId());
        }
        testModel.getQuestions().clear();

        if (questionRequests == null || questionRequests.isEmpty()) {
            return;
        }

        List<Question> newQuestions = questionRequests.stream()
                .map(request -> {
                    Question question = questionMapper.toQuestion(request);
                    question.setTest(testModel);
                    return question;
                })
                .toList();

        testModel.getQuestions().addAll(newQuestions);
    }
}
