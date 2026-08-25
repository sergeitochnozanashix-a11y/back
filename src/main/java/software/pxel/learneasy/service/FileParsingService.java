package software.pxel.learneasy.service;

import software.pxel.learneasy.api.dto.chat.AttachmentDto;

import java.util.concurrent.CompletableFuture;

public interface FileParsingService {

    CompletableFuture<String> parseFileAsync(AttachmentDto attachment);
}
