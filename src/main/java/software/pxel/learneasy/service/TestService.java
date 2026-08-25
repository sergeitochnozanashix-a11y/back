package software.pxel.learneasy.service;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import software.pxel.learneasy.api.dto.test.ExamDTO;
import software.pxel.learneasy.api.dto.test.TestRequest;
import software.pxel.learneasy.api.dto.test.TestResponse;

public interface TestService {
    TestResponse createTest(TestRequest testRequest);

    TestResponse getTestById(Long testId);

    TestResponse getTestByLessonId(Long lessonId);

    TestResponse updateTest(Long testId, TestRequest testRequest);

    void deleteTest(Long testId);

    Page<ExamDTO> getExamsByCourse(Long courseId, Long userId, Pageable pageable);
}
