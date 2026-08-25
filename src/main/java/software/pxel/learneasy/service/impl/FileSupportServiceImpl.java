package software.pxel.learneasy.service.impl;

import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import software.pxel.learneasy.service.FileSupportService;
import software.pxel.learneasy.service.parser.FileParser;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class FileSupportServiceImpl implements FileSupportService {

    private final List<FileParser> parsers;
    private Set<String> parsableExtensions;

    @PostConstruct
    private void init() {
        parsableExtensions = parsers.stream()
                .flatMap(parser -> parser.getSupportedExtensions().stream())
                .map(String::toLowerCase)
                .collect(Collectors.toUnmodifiableSet());
    }

    @Override
    public Set<String> getParsableFileExtensions() {
        return parsableExtensions;
    }
}
