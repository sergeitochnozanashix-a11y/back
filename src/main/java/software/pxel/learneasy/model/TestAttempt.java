package software.pxel.learneasy.model;

import jakarta.persistence.*;
import lombok.*;
import software.pxel.learneasy.model.enums.AttemptStatus;

import java.time.Instant;

@Entity
@Table(name = "test_attempts",
        indexes = {
                @Index(name = "idx_ta_user_test_created", columnList = "user_id,test_id,created_at DESC"),
                @Index(name = "idx_ta_status", columnList = "status"),
                @Index(name = "idx_ta_lesson_user", columnList = "lesson_id,user_id"),
                @Index(name = "idx_ta_module_user", columnList = "module_id,user_id")
        })
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TestAttempt extends AbstractAuditableEntity {

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "test_id", nullable = false)
    private TestModel test;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "lesson_id")
    private Lesson lesson;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "module_id", nullable = false)
    private Module module;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    @Builder.Default
    private AttemptStatus status = AttemptStatus.SUBMITTED;

    @Column(name = "score_percent")
    private Integer scorePercent;

    @Column(name = "passed")
    private Boolean passed;

    @Column(name = "evaluated_at")
    private Instant evaluatedAt;

    @Column(name = "failed_reason")
    private String failedReason;
}
