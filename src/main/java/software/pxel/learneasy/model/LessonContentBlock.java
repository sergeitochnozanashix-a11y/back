package software.pxel.learneasy.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.databind.JsonNode;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
import software.pxel.learneasy.model.enums.ContentBlockType;

import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "lesson_content_blocks")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class LessonContentBlock extends AbstractAuditableEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "lesson_id", nullable = false)
    @JsonIgnore
    private Lesson lesson;

    @Enumerated(EnumType.STRING)
    @Column(name = "block_type", nullable = false)
    private ContentBlockType blockType;

    @Column(columnDefinition = "TEXT")
    private String content;

    @Column(name = "sequence_order", nullable = false)
    private Integer sequenceOrder;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "properties", columnDefinition = "jsonb")
    private JsonNode properties;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "parent_id")
    private LessonContentBlock parent;

    @OneToMany(mappedBy = "parent", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<LessonContentBlock> children = new ArrayList<>();

    @Column(name = "file_url", length = 1024)
    private String fileUrl;

    @Column(name = "alt_text")
    private String altText;
}
