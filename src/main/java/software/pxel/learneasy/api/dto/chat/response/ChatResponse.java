package software.pxel.learneasy.api.dto.chat.response;

import io.swagger.v3.oas.annotations.media.Schema;
import software.pxel.learneasy.api.dto.chat.UserChatDto;

import java.util.List;

@Schema(
        description = "Ответ со списком чатов пользователя",
        example = """
                {
                  "userId": 123,
                  "userChatDtos": [
                    {
                      "chatId": 1,
                      "title": "Обсуждение проекта",
                      "updatedAt": "2025-10-09T18:30:00.456Z"
                    },
                    {
                      "chatId": 2,
                      "title": "Технические вопросы",
                      "updatedAt": "2025-10-09T17:28:00.123Z"
                    }
                  ]
                }
                """
)
public record ChatResponse(
        @Schema(
                description = "ID пользователя",
                example = "123"
        )
        Long userId,

        @Schema(
                description = "Список чатов пользователя, отсортированный по дате обновления (от новых к старым)"
        )
        List<UserChatDto> userChatDtos
) {
}
