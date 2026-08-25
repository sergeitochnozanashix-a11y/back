package software.pxel.learneasy.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import software.pxel.learneasy.model.Chat;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

@Repository
public interface ChatRepository extends JpaRepository<Chat, Long> {

    @Query("""
            SELECT new software.pxel.learneasy.repository.ChatRepository$ChatInfoProjection(c, MAX(cm.createdAt))
            FROM Chat c
            LEFT JOIN c.messages cm
            WHERE c.user.id = :userId
            GROUP BY c.id
            ORDER BY MAX(cm.createdAt) DESC NULLS LAST, c.updatedAt DESC
            """)
    List<ChatInfoProjection> findUserChatsWithLatestMessageTimestamp(@Param("userId") Long userId);

    @Query("select c from Chat c where c.id = :chatId and c.user.id = :userId")
    Optional<Chat> findByIdAndUserId(@Param("chatId") Long chatId, @Param("userId") Long userId);

    /**
     * DTO-проекция для инкапсуляции чата и времени его последнего сообщения.
     * Использование record обеспечивает неизменяемость и краткость.
     */
    record ChatInfoProjection(Chat chat, Instant latestMessageTimestamp) {
    }
}
