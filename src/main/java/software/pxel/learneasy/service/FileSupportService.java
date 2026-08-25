package software.pxel.learneasy.service;

import java.util.Set;

public interface FileSupportService {

    /**
     * Возвращает полный набор расширений файлов (без точки),
     * которые могут быть обработаны парсерами в системе.
     *
     * @return Неизменяемое множество строк с расширениями.
     */
    Set<String> getParsableFileExtensions();
}
