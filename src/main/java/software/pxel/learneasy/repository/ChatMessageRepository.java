package software.pxel.learneasy.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import org.springframework.data.repository.query.Param;
import software.pxel.learneasy.model.ChatMessage;

import java.util.List;
import java.util.Optional;

@Repository
public interface ChatMessageRepository extends JpaRepository<ChatMessage, Long> {

    @Query("SELECT cm FROM ChatMessage cm LEFT JOIN FETCH cm.attachments WHERE cm.chat.id = :chatId ORDER BY cm.createdAt ASC")
    List<ChatMessage> findByChatIdWithAttachments(@Param("chatId") Long chatId);

    @Query("SELECT cm FROM ChatMessage cm LEFT JOIN FETCH cm.attachments WHERE cm.id = :messageId")
    Optional<ChatMessage> findByIdWithAttachments(@Param("messageId") Long messageId);

    Optional<ChatMessage> findFirstByChatIdOrderByCreatedAtAsc(Long chatId);
}
