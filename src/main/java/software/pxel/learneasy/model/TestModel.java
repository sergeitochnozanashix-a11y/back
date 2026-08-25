package software.pxel.learneasy.model;

import jakarta.persistence.*;
import lombok.*;
import software.pxel.learneasy.model.enums.TestType;

import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "tests")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@ToString(exclude = {"lesson", "questions", "module"})
public class TestModel extends AbstractAuditableEntity {

    @Enumerated(EnumType.STRING)
    @Column(name = "test_type", nullable = false)
    @Builder.Default
    private TestType testType = TestType.LESSON_TEST;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "lesson_id", referencedColumnName = "id")
    private Lesson lesson;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "module_id", referencedColumnName = "id")
    private Module module;

    @Column(name = "title", nullable = false)
    @Builder.Default
    private String title = "Test";

    @Column(name = "pass_threshold_percentage", nullable = false)
    @Builder.Default
    private Integer passThresholdPercentage = 70;

    @OneToMany(
            mappedBy = "test",
            cascade = CascadeType.ALL,
            orphanRemoval = true,
            fetch = FetchType.LAZY
    )
    @OrderBy("sequenceOrder ASC")
    @Builder.Default
    private List<Question> questions = new ArrayList<>();

    public void addQuestion(Question question) {
        questions.add(question);
        question.setTest(this);
    }

    public void removeQuestion(Question question) {
        questions.remove(question);
        question.setTest(null);
    }

    public void setQuestions(List<Question> questions) {
        this.questions.clear();
        if (questions != null) {
            questions.forEach(this::addQuestion);
        }
    }
}
