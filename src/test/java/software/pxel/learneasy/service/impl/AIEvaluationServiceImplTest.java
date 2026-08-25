package software.pxel.learneasy.service.impl;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import software.pxel.learneasy.service.async.AttemptProcessor;

import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
@DisplayName("AIEvaluationService — управление очередью оценок")
class AIEvaluationServiceImplTest {

    @Mock
    private AttemptProcessor attemptProcessor;

    @InjectMocks
    private AIEvaluationServiceImpl service;

    @Test
    @DisplayName("evaluateAttempt(id) должен делегировать вызов в AttemptProcessor")
    void evaluateAttempt_shouldDelegateCallToAttemptProcessor() {
        // given
        Long attemptId = 123L;

        // when
        service.evaluateAttempt(attemptId);

        // then
        verify(attemptProcessor).evaluateAttempt(attemptId);
    }
}
