package software.pxel.learneasy.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import software.pxel.learneasy.model.Lesson;

import java.util.Optional;

@Repository
public interface LessonRepository extends JpaRepository<Lesson, Long> {

    @Query("SELECT l FROM Lesson l LEFT JOIN FETCH l.contentBlocks cb WHERE l.id = :lessonId ORDER BY l.createdAt ASC")
    Optional<Lesson> findByIdWithContentBlock(Long lessonId);

    @Query("SELECT MAX(l.sequenceOrder) FROM Lesson l WHERE l.module.id = :moduleId")
    Optional<Integer> findMaxSequenceOrderByModuleId(Long moduleId);

    @Query("SELECT l FROM Lesson l WHERE l.module.id = :moduleId")
    Page<Lesson> findAllByModuleIdWithFilters(
            @Param("moduleId") Long moduleId,
            Pageable pageable
    );
}
