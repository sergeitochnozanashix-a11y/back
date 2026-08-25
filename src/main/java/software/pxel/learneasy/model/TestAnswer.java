package software.pxel.learneasy.model;

import jakarta.persistence.*;
import lombok.*;
import software.pxel.learneasy.model.enums.AnswerEvaluation;
import software.pxel.learneasy.model.enums.AnswerInputType;

@Entity
@Table(name = "test_answers",
        uniqueConstraints = @UniqueConstraint(name = "uq_tans_attempt_question", columnNames = {"attempt_id", "question_id"}),
        indexes = {
                @Index(name = "idx_tans_attempt", columnList = "attempt_id"),
                @Index(name = "idx_tans_question", columnList = "question_id"),
                @Index(name = "idx_tans_eval", columnList = "evaluation")
        })
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TestAnswer extends AbstractAuditableEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "attempt_id", nullable = false)
    private TestAttempt attempt;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "question_id", nullable = false)
    private Question question;

    @Enumerated(EnumType.STRING)
    @Column(name = "input_type")
    private AnswerInputType inputType;

    @Column(name = "text_answer")
    private String textAnswer;

    @Column(name = "voice_file_url")
    private String voiceFileUrl;

    @Column(name = "voice_transcript")
    private String voiceTranscript;

    @Enumerated(EnumType.STRING)
    @Column(name = "evaluation", nullable = false)
    @Builder.Default
    private AnswerEvaluation evaluation = AnswerEvaluation.UNASSESSED;

    @Column(name = "evaluation_notes", columnDefinition = "TEXT")
    private String evaluationNotes;
}
