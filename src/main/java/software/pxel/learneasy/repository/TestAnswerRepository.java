package software.pxel.learneasy.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import software.pxel.learneasy.model.TestAnswer;
import software.pxel.learneasy.model.TestAttempt;
import software.pxel.learneasy.model.enums.AnswerEvaluation;

import java.util.List;

public interface TestAnswerRepository extends JpaRepository<TestAnswer, Long> {

    @Query("SELECT a FROM TestAnswer a LEFT JOIN FETCH a.question WHERE a.attempt.id = :attemptId")
    List<TestAnswer> findByAttemptIdWithQuestion(Long attemptId);

    @Query("SELECT a FROM TestAnswer a LEFT JOIN FETCH a.question WHERE a.attempt IN :attempts")
    List<TestAnswer> findByAttemptInWithQuestion(List<TestAttempt> attempts);

    Integer countByAttemptIdAndEvaluation(Long attemptId, AnswerEvaluation evaluation);
}
