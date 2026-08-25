package software.pxel.learneasy.service.parser.impl;

import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import software.pxel.learneasy.service.parser.FileParser;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.Set;
import java.util.stream.Collectors;

@Order(10)
@Component
public class CsvParser implements FileParser {

    private static final String CSV_EXTENSION = "csv";

    @Override
    public String parse(InputStream inputStream) throws IOException {
        StringBuilder sb = new StringBuilder();

        CSVFormat format = CSVFormat.Builder.create(CSVFormat.DEFAULT)
                .setHeader()
                .build();

        try (InputStreamReader reader = new InputStreamReader(inputStream, StandardCharsets.UTF_8);
             CSVParser csvParser = new CSVParser(reader, format)) {

            String header = String.join(" | ", csvParser.getHeaderNames());
            sb.append("| ").append(header).append(" |\n");

            String separator = csvParser.getHeaderNames().stream()
                    .map(h -> "---").collect(Collectors.joining(" | "));
            sb.append("| ").append(separator).append(" |\n");

            for (CSVRecord csvRecord : csvParser) {
                String row = csvParser.getHeaderNames().stream()
                        .map(csvRecord::get)
                        .collect(Collectors.joining(" | "));
                sb.append("| ").append(row).append(" |\n");
            }
        }
        return sb.toString();
    }

    @Override
    public boolean supports(String contentType, String filename) {
        return filename != null && filename.toLowerCase().endsWith("." + CSV_EXTENSION);
    }

    @Override
    public Set<String> getSupportedExtensions() {
        return Set.of(CSV_EXTENSION);
    }
}
