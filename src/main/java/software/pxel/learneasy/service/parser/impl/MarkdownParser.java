package software.pxel.learneasy.service.parser.impl;

import org.commonmark.node.Node;
import org.commonmark.parser.Parser;
import org.commonmark.renderer.text.TextContentRenderer;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import software.pxel.learneasy.service.parser.FileParser;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.Set;

import static software.pxel.learneasy.service.parser.constant.SupportedExtension.FORMAT_FILE_MD;

@Order(10)
@Component
public class MarkdownParser implements FileParser {

    private static final String MD_EXTENSION = "md";

    @Override
    public String parse(InputStream inputStream) throws IOException {
        Parser parser = Parser.builder().build();
        TextContentRenderer renderer = TextContentRenderer.builder().build();
        try (InputStreamReader reader = new InputStreamReader(inputStream, StandardCharsets.UTF_8)) {
            Node document = parser.parseReader(reader);
            return renderer.render(document);
        }
    }

    @Override
    public boolean supports(String contentType, String filename) {
        return filename != null && filename.toLowerCase().endsWith(FORMAT_FILE_MD);
    }

    @Override
    public Set<String> getSupportedExtensions() {
        return Set.of(MD_EXTENSION);
    }
}
