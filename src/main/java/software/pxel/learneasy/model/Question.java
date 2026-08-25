package software.pxel.learneasy.model;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "test_question")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@ToString(exclude = "test")
public class Question extends AbstractAuditableEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "test_id", nullable = false)
    private TestModel test;

    @Column(name = "text", nullable = false, columnDefinition = "TEXT")
    private String text;

    @Column(name = "sequence_order", nullable = false)
    @Builder.Default
    private Integer sequenceOrder = 0;

    @Column(name = "max_score", nullable = false)
    @Builder.Default
    private Integer maxScore = 2;
}
