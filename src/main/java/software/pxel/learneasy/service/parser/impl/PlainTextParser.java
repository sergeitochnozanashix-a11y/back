package software.pxel.learneasy.service.parser.impl;

import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import software.pxel.learneasy.service.parser.FileParser;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Set;

import static software.pxel.learneasy.service.parser.constant.SupportedExtension.SUPPORTED_EXTENSIONS;

@Order(100)
@Component
public class PlainTextParser implements FileParser {


    @Override
    public String parse(InputStream inputStream) throws IOException {
        return new String(inputStream.readAllBytes(), StandardCharsets.UTF_8);
    }

    @Override
    public boolean supports(String contentType, String filename) {
        String extension = getExtension(filename).toLowerCase();
        return SUPPORTED_EXTENSIONS.contains(extension);
    }

    @Override
    public Set<String> getSupportedExtensions() {
        return SUPPORTED_EXTENSIONS;
    }

    private String getExtension(String filename) {
        if (filename == null || filename.lastIndexOf('.') == -1) {
            return "";
        }
        return filename.substring(filename.lastIndexOf('.') + 1);
    }
}
