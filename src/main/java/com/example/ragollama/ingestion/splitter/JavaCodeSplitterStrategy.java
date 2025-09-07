package com.example.ragollama.ingestion.splitter;

import com.github.javaparser.JavaParser;
import com.github.javaparser.ParserConfiguration;
import com.github.javaparser.ast.body.MethodDeclaration;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.document.Document;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Стратегия разделения, специализированная для Java-кода.
 * <p>
 * Использует Abstract Syntax Tree (AST), построенный с помощью
 * библиотеки {@link JavaParser}, для семантически корректного разделения
 * исходного кода на чанки. Каждый публичный метод класса, вместе с его
 * Javadoc и аннотациями, становится отдельным, семантически целостным
 * документом-чанком.
 * <p>
 * Эта стратегия имеет высокий приоритет (низкое значение {@code @Order}),
 * чтобы она применялась к Java-файлам раньше, чем fallback-стратегии.
 */
@Component
@Order(10)
@Slf4j
public class JavaCodeSplitterStrategy implements DocumentSplitterStrategy {

    private final JavaParser javaParser;

    /**
     * Конструктор, инициализирующий парсер с поддержкой Java 21.
     */
    public JavaCodeSplitterStrategy() {
        this.javaParser = new JavaParser(new ParserConfiguration().setLanguageLevel(ParserConfiguration.LanguageLevel.JAVA_21));
    }

    /**
     * {@inheritDoc}
     *
     * @param document Документ для проверки.
     * @return {@code true}, если имя источника документа заканчивается на ".java".
     */
    @Override
    public boolean supports(Document document) {
        Object source = document.getMetadata().get("source");
        return source instanceof String && ((String) source).endsWith(".java");
    }

    /**
     * {@inheritDoc}
     *
     * @param document Исходный документ с Java-кодом.
     * @param config   Конфигурация (в данной реализации не используется, так как
     *                 деление происходит по семантическим границам методов).
     * @return Список документов-чанков, где каждый чанк — это один метод.
     */
    @Override
    public List<Document> split(Document document, SplitterConfig config) {
        log.debug("Применение JavaCodeSplitterStrategy для документа: {}", document.getMetadata().get("source"));
        return javaParser.parse(document.getText()).getResult()
                .map(cu -> cu.findAll(MethodDeclaration.class).stream()
                        .filter(MethodDeclaration::isPublic) // Индексируем только публичные методы
                        .map(method -> {
                            // Создаем новый документ для метода, наследуя метаданные
                            Map<String, Object> newMetadata = new java.util.HashMap<>(document.getMetadata());
                            newMetadata.put("method_name", method.getNameAsString());
                            newMetadata.put("start_line", method.getBegin().map(p -> p.line).orElse(-1));
                            return new Document(method.toString(), newMetadata);
                        })
                        .collect(Collectors.toList()))
                .orElseGet(() -> {
                    log.warn("Не удалось распарсить Java-код для документа: {}. Будет создан один чанк.", document.getMetadata().get("source"));
                    return List.of(document); // Fallback: если парсинг не удался, возвращаем весь файл как один чанк
                });
    }

    /**
     * {@inheritDoc}
     *
     * @return Приоритет 10.
     */
    @Override
    public int getOrder() {
        return 10;
    }
}
