package software.pxel.learneasy.service.impl;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import software.pxel.learneasy.api.dto.question.QuestionRequest;
import software.pxel.learneasy.mapper.QuestionMapper;
import software.pxel.learneasy.model.Question;
import software.pxel.learneasy.model.TestModel;
import software.pxel.learneasy.repository.QuestionRepository;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("QuestionService — замена вопросов теста")
class QuestionServiceImplTest {

    @Mock
    QuestionRepository questionRepository;
    @Mock
    QuestionMapper questionMapper;

    @InjectMocks
    QuestionServiceImpl service;

    // ------------------------------------------------------------
    @Nested
    @DisplayName("replaceQuestionsForTest(test, requests)")
    class ReplaceQuestions {

        @Test
        @DisplayName("успех — test.id != null: удаляет старые, очищает, маппит и добавляет новые в том же порядке")
        void persistedTest_deletesAndAddsInOrder() {
            TestModel test = new TestModel();
            test.setId(100L);
            test.getQuestions().add(new Question());

            QuestionRequest r1 = new QuestionRequest("q1", 1, 2);
            QuestionRequest r2 = new QuestionRequest("q2", 2, 3);

            Question q1 = new Question();
            Question q2 = new Question();
            when(questionMapper.toQuestion(r1)).thenReturn(q1);
            when(questionMapper.toQuestion(r2)).thenReturn(q2);

            service.replaceQuestionsForTest(test, List.of(r1, r2));

            verify(questionRepository).deleteAllByTestId(100L);

            assertEquals(2, test.getQuestions().size());
            assertSame(q1, test.getQuestions().get(0));
            assertSame(q2, test.getQuestions().get(1));

            assertSame(test, q1.getTest());
            assertSame(test, q2.getTest());

            verify(questionMapper).toQuestion(r1);
            verify(questionMapper).toQuestion(r2);
            verifyNoMoreInteractions(questionRepository, questionMapper);
        }

        @Test
        @DisplayName("успех — test.id == null: не вызывает deleteAllByTestId, только очистка и добавление")
        void newTest_onlyClearsAndAdds() {
            TestModel test = new TestModel();
            test.getQuestions().add(new Question());

            QuestionRequest r = new QuestionRequest("q", 1, 2);
            Question q = new Question();
            when(questionMapper.toQuestion(r)).thenReturn(q);

            service.replaceQuestionsForTest(test, List.of(r));

            verify(questionRepository, never()).deleteAllByTestId(anyLong());
            assertEquals(1, test.getQuestions().size());
            assertSame(q, test.getQuestions().getFirst());
            assertSame(test, q.getTest());
            verify(questionMapper).toQuestion(r);
            verifyNoMoreInteractions(questionRepository, questionMapper);
        }

        @Test
        @DisplayName("успех — пустой список: удаляет (если id != null), очищает и ничего не добавляет")
        void emptyList_deletesIfPersisted_andClears_only() {
            TestModel test = new TestModel();
            test.setId(7L);
            test.getQuestions().add(new Question());

            service.replaceQuestionsForTest(test, List.of());

            verify(questionRepository).deleteAllByTestId(7L);
            assertTrue(test.getQuestions().isEmpty());
            verifyNoInteractions(questionMapper);
            verifyNoMoreInteractions(questionRepository);
        }

        @Test
        @DisplayName("успех — null вместо списка: ведёт себя как пустой (delete при id, очистка, без маппинга)")
        void nullList_treatedAsEmpty() {
            TestModel test = new TestModel();
            test.setId(8L);
            test.getQuestions().add(new Question());

            service.replaceQuestionsForTest(test, null);

            verify(questionRepository).deleteAllByTestId(8L);
            assertTrue(test.getQuestions().isEmpty());
            verifyNoInteractions(questionMapper);
            verifyNoMoreInteractions(questionRepository);
        }

        @Test
        @DisplayName("инвариант — в тесте после замены лежат именно те инстансы, что вернул маппер")
        void holdsExactlyMapperInstances() {
            TestModel test = new TestModel();
            test.setId(55L);

            QuestionRequest r1 = new QuestionRequest("A", 0, 1);
            QuestionRequest r2 = new QuestionRequest("B", 1, 1);
            Question rq1 = new Question();
            Question rq2 = new Question();
            when(questionMapper.toQuestion(r1)).thenReturn(rq1);
            when(questionMapper.toQuestion(r2)).thenReturn(rq2);

            service.replaceQuestionsForTest(test, List.of(r1, r2));

            assertTrue(test.getQuestions().contains(rq1));
            assertTrue(test.getQuestions().contains(rq2));
            assertSame(test, rq1.getTest());
            assertSame(test, rq2.getTest());
        }
    }
}
