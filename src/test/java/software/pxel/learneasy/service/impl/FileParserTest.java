package software.pxel.learneasy.service.impl;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import software.pxel.learneasy.service.parser.impl.CsvParser;
import software.pxel.learneasy.service.parser.impl.MarkdownParser;
import software.pxel.learneasy.service.parser.impl.PlainTextParser;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

@DisplayName("Тесты для сервисов парсинга файлов")
public class FileParserTest {

    @Nested
    @DisplayName("PlainTextParser")
    class PlainTextParserTests {
        private final PlainTextParser parser = new PlainTextParser();

        @Test
        @DisplayName("Должен читать содержимое как есть")
        void shouldReadContentAsIs() throws IOException {
            String textContent = "Это обычный текст.\nС несколькими строками.";
            InputStream inputStream = new ByteArrayInputStream(textContent.getBytes(StandardCharsets.UTF_8));
            String result = parser.parse(inputStream);
            assertEquals(textContent, result);
        }

        @Test
        @DisplayName("Должен поддерживать все текстовые форматы")
        void shouldSupportTextFormats() {
            assertTrue(parser.supports("text/plain", "file.txt"));
            assertTrue(parser.supports("application/java", "MyClass.java"));
            assertTrue(parser.supports("application/javascript", "script.js"));
            assertTrue(parser.supports("application/json", "data.json"));
            assertTrue(parser.supports("application/xml", "config.xml"));
            assertTrue(parser.supports("text/yaml", "config.yml"));
            assertTrue(parser.supports("text/html", "index.html"));
            assertTrue(parser.supports("text/css", "style.css"));
            assertTrue(parser.supports("text/x-log", "app.log"));
            assertFalse(parser.supports("application/pdf", "document.pdf"));
        }
    }

    @Nested
    @DisplayName("MarkdownParser")
    class MarkdownParserTests {
        private final MarkdownParser parser = new MarkdownParser();

        @Test
        @DisplayName("Должен извлекать текст, сохраняя кавычки и маркеры списков")
        void shouldExtractTextAndRemoveMarkup() throws IOException {
            String markdownContent = """
                    # Заголовок
                    Это **жирный** текст и [ссылка](https://example.com).
                    - Элемент списка
                    """;
            InputStream inputStream = new ByteArrayInputStream(markdownContent.getBytes(StandardCharsets.UTF_8));
            String result = parser.parse(inputStream);

            String expectedText = """
                    Заголовок
                    Это жирный текст и "ссылка" (https://example.com).
                    - Элемент списка
                    """;

            assertThat(result.trim()).isEqualTo(expectedText.trim());
        }

        @Test
        @DisplayName("Должен поддерживать только .md файлы")
        void shouldSupportOnlyMdFiles() {
            assertTrue(parser.supports("text/markdown", "document.md"));
            assertTrue(parser.supports("text/plain", "README.MD"));
            assertFalse(parser.supports("text/plain", "document.txt"));
        }
    }

    @Nested
    @DisplayName("CsvParser")
    class CsvParserTests {
        private final CsvParser parser = new CsvParser();

        @Test
        @DisplayName("Должен преобразовывать CSV в Markdown-таблицу")
        void shouldConvertCsvToMarkdownTable() throws IOException {
            String csvContent = """
                    Name,Email,Role
                    John Doe,john@example.com,Admin
                    Jane Smith,jane@example.com,User
                    """;
            InputStream inputStream = new ByteArrayInputStream(csvContent.getBytes(StandardCharsets.UTF_8));
            String result = parser.parse(inputStream);

            String expectedMarkdown = """
                    | Name | Email | Role |
                    | --- | --- | --- |
                    | John Doe | john@example.com | Admin |
                    | Jane Smith | jane@example.com | User |
                    """;

            assertEquals(
                    expectedMarkdown.replaceAll("\\s+", "").trim(),
                    result.replaceAll("\\s+", "").trim()
            );
        }

        @Test
        @DisplayName("Должен поддерживать только .csv файлы")
        void shouldSupportOnlyCsvFiles() {
            assertTrue(parser.supports("text/csv", "data.csv"));
            assertTrue(parser.supports("application/vnd.ms-excel", "REPORT.CSV"));
            assertFalse(parser.supports("text/plain", "data.txt"));
        }
    }
}
