package software.pxel.learneasy.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import software.pxel.learneasy.model.Question;

public interface QuestionRepository extends JpaRepository<Question, Long> {
    void deleteAllByTestId(Long testId);
}
