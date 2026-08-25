package software.pxel.learneasy.mapper;

import org.mapstruct.Builder;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import software.pxel.learneasy.api.dto.chat.AttachmentDto;
import software.pxel.learneasy.api.dto.chat.UserChatDto;
import software.pxel.learneasy.api.dto.chat.response.AttachmentResponseDto;
import software.pxel.learneasy.api.dto.chat.response.MessageResponseDto;
import software.pxel.learneasy.feign.ai.dto.ChatMessageDto;
import software.pxel.learneasy.model.ChatMessage;
import software.pxel.learneasy.model.MessageAttachment;
import software.pxel.learneasy.repository.ChatRepository.ChatInfoProjection;

@Mapper(componentModel = "spring", builder = @Builder(disableBuilder = true))
public interface ChatMapper {

    @Mapping(source = "chat.id", target = "chatId")
    @Mapping(source = "chat.title", target = "title")
    @Mapping(source = "latestMessageTimestamp", target = "updatedAt")
    UserChatDto chatInfoProjectionToUserChatDto(ChatInfoProjection projection);

    @Mapping(source = "role", target = "role")
    @Mapping(source = "attachments", target = "attachments")
    @Mapping(source = "voiceTranscript", target = "voiceTranscript")
    ChatMessageDto chatMessageEntityToChatMessageDto(ChatMessage chatMessage);

    MessageResponseDto chatMessageEntityToMessageResponseDto(ChatMessage chatMessage);

    AttachmentResponseDto messageAttachmentEntityToAttachmentResponseDto(MessageAttachment attachment);

    AttachmentDto attachmentEntityToAttachmentDto(MessageAttachment attachment);
}
