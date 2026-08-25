package software.pxel.learneasy.service;

import software.pxel.learneasy.api.dto.chat.request.SendMessageRequest;
import software.pxel.learneasy.api.dto.chat.response.ChatMessagesResponse;
import software.pxel.learneasy.api.dto.chat.response.ChatResponse;

public interface ChatService {

    Long createNewChat();

    ChatResponse getListChatsUserSortDate();

    ChatMessagesResponse getAllMessageByChatId(Long chatId);

    void sendingMessageToChat(Long chatId, SendMessageRequest request);
}
