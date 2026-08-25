package software.pxel.learneasy.service.parser;

import java.io.IOException;
import java.io.InputStream;
import java.util.Set;

public interface FileParser {

    /**
     * Извлекает текстовое содержимое из потока данных.
     *
     * @param inputStream Поток данных файла.
     * @return Распознанный текст в виде строки.
     * @throws IOException если произошла ошибка чтения.
     */
    String parse(InputStream inputStream) throws IOException;

    /**
     * Проверяет, поддерживает ли данный парсер указанный файл.
     *
     * @param contentType MIME-тип файла.
     * @param filename    Имя файла.
     * @return {@code true}, если формат поддерживается, иначе {@code false}.
     */
    boolean supports(String contentType, String filename);

    /**
     * Возвращает набор поддерживаемых расширений файлов (без точки).
     *
     * @return Множество строк с расширениями.
     */
    Set<String> getSupportedExtensions();
}
