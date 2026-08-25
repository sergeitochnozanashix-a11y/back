package software.pxel.learneasy.service.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import software.pxel.learneasy.api.dto.chat.AttachmentDto;
import software.pxel.learneasy.service.FileParsingService;
import software.pxel.learneasy.service.parser.FileParser;

import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URL;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;

@Slf4j
@Service
@RequiredArgsConstructor
public class FileParsingServiceImpl implements FileParsingService {

    private final List<FileParser> parsers;
    private final Executor ioExecutor;

    @Override
    public CompletableFuture<String> parseFileAsync(AttachmentDto attachment) {
        log.info("Submitting file for async parsing: {}", attachment.originalFilename());

        Optional<FileParser> parserOpt = findParserFor(attachment);

        if (parserOpt.isEmpty()) {
            String unsupportedMsg = String.format("[File %s has an unsupported format and could not be parsed]", attachment.originalFilename());
            log.warn(unsupportedMsg);
            return CompletableFuture.completedFuture(unsupportedMsg);
        }

        FileParser parser = parserOpt.get();

        return CompletableFuture.supplyAsync(() -> {
            log.info("Executing async parsing for: {}", attachment.originalFilename());
            try {
                URI uri = URI.create(attachment.downloadUrl());
                URL url = uri.toURL();
                try (InputStream is = url.openStream()) {
                    return parser.parse(is);
                }
            } catch (MalformedURLException | IllegalArgumentException e) {
                log.error("Invalid URL for file '{}': {}", attachment.originalFilename(), attachment.downloadUrl(), e);
                return formatErrorMessage(attachment, "Invalid URL");
            } catch (IOException e) {
                log.error("Failed to download or read file '{}'", attachment.originalFilename(), e);
                return formatErrorMessage(attachment, "I/O Error");
            } catch (Exception e) {
                log.error("Unexpected error parsing file '{}' with parser {}",
                        attachment.originalFilename(), parser.getClass().getSimpleName(), e);
                return formatErrorMessage(attachment, e.getMessage());
            }
        }, ioExecutor);
    }

    /**
     * Находит подходящий парсер для файла.
     */
    private Optional<FileParser> findParserFor(AttachmentDto attachment) {
        return parsers.stream()
                .filter(p -> p.supports(attachment.contentType(), attachment.originalFilename()))
                .findFirst();
    }

    /**
     * Форматирует стандартное сообщение об ошибке для включения в контекст AI.
     */
    private String formatErrorMessage(AttachmentDto attachment, String reason) {
        return String.format("[Error parsing file %s: %s]", attachment.originalFilename(), reason);
    }
}
