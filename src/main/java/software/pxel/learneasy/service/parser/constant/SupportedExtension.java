package software.pxel.learneasy.service.parser.constant;

import java.util.Set;

public class SupportedExtension {

    public static final String START_WITH_CONTENT_TYPE = "audio/";
    public static final String FORMAT_FILE_MD = ".md";
    public static final Set<String> SUPPORTED_EXTENSIONS = Set.of(
            "txt", "java", "py", "js", "json", "xml", "yml", "yaml", "html", "css", "log"
    );
}
