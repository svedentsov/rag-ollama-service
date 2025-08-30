package com.example.ragollama.agent.compliance.tool;

import com.example.ragollama.agent.compliance.model.ScannedDependency;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Сервис-инструмент, имитирующий работу сканера зависимостей (SCA tool).
 * <p>
 * ВАЖНО: Эта реализация является **mock-заглушкой** для демонстрации.
 * В реальном проекте здесь будет интеграция с полноценным инструментом,
 * таким как Gradle License Report Plugin или OWASP Dependency-Check,
 * который бы возвращал полный граф зависимостей с их лицензиями.
 */
@Slf4j
@Service
public class DependencyScannerService {

    // Паттерн для извлечения 'group:name:version' из строки implementation
    private static final Pattern DEPENDENCY_PATTERN =
            Pattern.compile("\\b(implementation|api|compileOnly|runtimeOnly)\\s*['\"]([^:'\"]+):([^:'\"]+):([^:'\"]+)['\"]");

    /**
     * Сканирует содержимое файла сборки и извлекает список зависимостей.
     *
     * @param buildFileContent Содержимое файла `build.gradle`.
     * @return Список обнаруженных зависимостей с mock-лицензиями.
     */
    public List<ScannedDependency> scan(String buildFileContent) {
        log.info("Запуск mock-сканера зависимостей...");
        List<ScannedDependency> dependencies = new ArrayList<>();
        Matcher matcher = DEPENDENCY_PATTERN.matcher(buildFileContent);

        while (matcher.find()) {
            String group = matcher.group(2);
            String name = matcher.group(3);
            String version = matcher.group(4);
            // Mock-логика для определения лицензии
            String license = getMockLicenseFor(name);
            dependencies.add(new ScannedDependency(group, name, version, license));
        }
        log.info("Mock-сканер обнаружил {} прямых зависимостей.", dependencies.size());
        return dependencies;
    }

    /**
     * Возвращает предопределенную лицензию для демонстрации.
     */
    private String getMockLicenseFor(String name) {
        if (name.contains("spring-boot-starter")) {
            return "Apache 2.0";
        }
        if (name.contains("lombok")) {
            return "MIT";
        }
        if (name.contains("jgit")) {
            return "EDL-1.0"; // Eclipse Distribution License
        }
        // Симулируем "проблемную" зависимость
        if (name.contains("some-problematic-library")) {
            return "GPL-3.0";
        }
        return "Unknown";
    }
}
