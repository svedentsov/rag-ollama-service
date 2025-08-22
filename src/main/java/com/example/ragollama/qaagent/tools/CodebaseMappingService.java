package com.example.ragollama.qaagent.tools;

import org.springframework.stereotype.Service;

import java.util.Optional;

/**
 * Утилитарный сервис для сопоставления файлов исходного кода с их тестовыми файлами.
 * <p>
 * Инкапсулирует эвристики, специфичные для структуры проекта (в данном случае,
 * стандартная структура Maven/Gradle). Изоляция этой логики в отдельном
 * сервисе позволяет легко изменять ее в будущем, не затрагивая агентов.
 */
@Service
public class CodebaseMappingService {

    /**
     * Находит соответствующий тестовый файл для файла приложения.
     * <p>
     * Реализует простую эвристику: заменяет {@code /main/} на {@code /test/}
     * и добавляет суффикс 'Test' к имени файла.
     *
     * @param appFilePath Путь к файлу в 'src/main/...'.
     * @return {@link Optional} с путем к тестовому файлу или пустой,
     * если соответствие не найдено или входные данные некорректны.
     */
    public Optional<String> findTestForAppFile(String appFilePath) {
        if (appFilePath == null || !appFilePath.contains("src/main/java")) {
            return Optional.empty();
        }

        String testPath = appFilePath
                .replace("src/main/java", "src/test/java")
                .replace(".java", "Test.java");

        return Optional.of(testPath);
    }
}
