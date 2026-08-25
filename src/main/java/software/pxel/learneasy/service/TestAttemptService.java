package software.pxel.learneasy.service;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import software.pxel.learneasy.api.dto.attempt.CreateAttemptRequest;
import software.pxel.learneasy.api.dto.attempt.TestAttemptResponse;

public interface TestAttemptService {
    TestAttemptResponse createAttempt(Long userId, CreateAttemptRequest request);

    TestAttemptResponse getLastAttempt(Long userId, Long testId);

    Page<TestAttemptResponse> getAttempts(Long userId, Long testId, Pageable pageable);
}
