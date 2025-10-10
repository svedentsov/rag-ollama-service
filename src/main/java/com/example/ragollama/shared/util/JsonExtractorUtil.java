package com.example.ragollama.shared.util;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

/**
 * Утилитарный компонент для надежного извлечения JSON-блоков из текста,
 * который может содержать артефакты Markdown или другие посторонние символы.
 * <p>
 * Реализует несколько эвристик для повышения надежности парсинга:
 * <ol>
 *     <li>Поиск JSON-блоков внутри Markdown-разметки (```json ... ```).</li>
 *     <li>Поиск всех потенциальных JSON-объектов и массивов путем балансировки скобок.</li>
 *     <li>Проверка каждого кандидата на валидность с помощью "снисходительного" парсера.</li>
 * </ol>
 * Использование в качестве Spring-компонента гарантирует, что для парсинга
 * будет использован корректно сконфигурированный {@link ObjectMapper}.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class JsonExtractorUtil {

    /**
     * "Снисходительный" ObjectMapper, который допускает некоторые отклонения от стандарта JSON.
     * Внедряется из AppConfig.
     */
    @Qualifier("permissiveObjectMapper")
    private final ObjectMapper permissiveMapper;

    /**
     * Извлекает первый валидный JSON-блок (объект или массив) из текста.
     *
     * @param text Ответ от LLM, потенциально содержащий "мусор".
     * @return Строка с валидным JSON или пустая строка, если JSON не найден.
     */
    public String extractJsonBlock(String text) {
        if (text == null || text.isBlank()) {
            return "";
        }

        String fromMarkdown = extractFromMarkdown(text);
        if (!fromMarkdown.isEmpty() && isValidJson(fromMarkdown)) {
            return fromMarkdown;
        }

        List<String> candidates = extractAllByBalancingBrackets(text);
        for (String candidate : candidates) {
            if (isValidJson(candidate)) {
                return candidate;
            }
        }

        int firstBrace = firstBracketIndex(text);
        if (firstBrace != -1) {
            String fallback = extractByBalancingFromIndex(text, firstBrace);
            if (!fallback.isEmpty() && isValidJson(fallback)) {
                return fallback;
            }
        }

        log.warn("JsonExtractorUtil: не удалось извлечь валидный JSON из ответа LLM. Отрезок (макс 1000): {}",
                StringUtils.left(text, 1000));
        return "";
    }

    private String extractFromMarkdown(String text) {
        String jsonBlock = StringUtils.substringBetween(text, "```json", "```");
        if (jsonBlock != null) {
            return jsonBlock.trim();
        }
        jsonBlock = StringUtils.substringBetween(text, "```", "```");
        if (jsonBlock != null) {
            String trimmed = jsonBlock.trim();
            if (trimmed.startsWith("{") || trimmed.startsWith("[")) {
                return trimmed;
            }
        }
        return "";
    }

    private List<String> extractAllByBalancingBrackets(String text) {
        List<String> result = new ArrayList<>();
        char[] chars = text.toCharArray();

        for (int i = 0; i < chars.length; i++) {
            if (chars[i] == '{' || chars[i] == '[') {
                String candidate = extractByBalancingFromIndex(text, i);
                if (!candidate.isEmpty()) {
                    result.add(candidate);
                    i += candidate.length() - 1;
                }
            }
        }
        return result;
    }

    private int firstBracketIndex(String text) {
        int braceIndex = text.indexOf('{');
        int bracketIndex = text.indexOf('[');
        if (braceIndex == -1) return bracketIndex;
        if (bracketIndex == -1) return braceIndex;
        return Math.min(braceIndex, bracketIndex);
    }

    private String extractByBalancingFromIndex(String text, int start) {
        if (start < 0 || start >= text.length()) return "";

        char open = text.charAt(start);
        char close = open == '{' ? '}' : ']';
        int balance = 1;

        for (int i = start + 1; i < text.length(); i++) {
            char c = text.charAt(i);
            if (c == open) balance++;
            else if (c == close) balance--;
            if (balance == 0) {
                return text.substring(start, i + 1).trim();
            }
        }
        return "";
    }

    private boolean isValidJson(String json) {
        if (json == null || json.isBlank()) return false;
        try {
            permissiveMapper.readTree(json);
            return true;
        } catch (Exception ex) {
            return false;
        }
    }
}
