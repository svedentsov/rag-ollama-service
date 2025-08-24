package com.example.ragollama.shared.util;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.experimental.UtilityClass;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;

import java.util.ArrayList;
import java.util.List;

@Slf4j
@UtilityClass
public class JsonExtractorUtil {

    // Строгий mapper (по умолчанию)
    private static final ObjectMapper STRICT_MAPPER = new ObjectMapper();

    // Более терпимый mapper — на случай, если LLM вернул небольшие отклонения от строгого JSON
    private static final ObjectMapper PERMISSIVE_MAPPER = new ObjectMapper()
            .configure(JsonParser.Feature.ALLOW_COMMENTS, true)
            .configure(JsonParser.Feature.ALLOW_SINGLE_QUOTES, true)
            .configure(JsonParser.Feature.ALLOW_UNQUOTED_FIELD_NAMES, true)
            .configure(JsonParser.Feature.ALLOW_TRAILING_COMMA, true);

    /**
     * Возвращает первый валидный JSON (объект или массив) найденный в тексте.
     * Попытки: fenced markdown -> все сбалансированные блоки -> в конце попытка взять
     * подстроку от первой скобки до соответствующего конца.
     *
     * @param text ответ LLM
     * @return валидный JSON как строка или "" если не найден
     */
    public static String extractJsonBlock(String text) {
        if (text == null || text.isBlank()) {
            return "";
        }

        // 1) fenced markdown
        String fromMarkdown = extractFromMarkdown(text);
        if (!fromMarkdown.isEmpty()) {
            if (isValidJson(fromMarkdown, true)) return fromMarkdown;
            if (isValidJson(fromMarkdown, false)) return fromMarkdown;
            // если fenced блок невалиден — у нас будут другие шаги
        }

        // 2) Найти все сбалансированные блоки ({} и [])
        List<String> candidates = extractAllByBalancingBrackets(text);
        for (String c : candidates) {
            if (isValidJson(c, true)) return c;  // строгий
        }
        for (String c : candidates) {
            if (isValidJson(c, false)) return c; // терпимый
        }

        // 3) Fallback: получить подстроку от первой открывающей до конца и попробовать балансировать до найденного конца
        int firstBrace = firstBracketIndex(text);
        if (firstBrace != -1) {
            String fallback = extractByBalancingFromIndex(text, firstBrace);
            if (!fallback.isEmpty()) {
                if (isValidJson(fallback, true)) return fallback;
                if (isValidJson(fallback, false)) return fallback;
            }
        }

        // не нашли ничего
        log.warn("JsonExtractorUtil: не удалось извлечь валидный JSON из ответа LLM. Отрезок (макс 1000): {}",
                StringUtils.left(text, 1000));
        return "";
    }

    private static String extractFromMarkdown(String text) {
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

    private static List<String> extractAllByBalancingBrackets(String text) {
        List<String> result = new ArrayList<>();
        char[] chars = text.toCharArray();

        for (int i = 0; i < chars.length; i++) {
            if (chars[i] == '{' || chars[i] == '[') {
                String candidate = extractByBalancingFromIndex(text, i);
                if (!candidate.isEmpty()) {
                    result.add(candidate);
                    // прыгаем i вперед, чтобы не возвращать вложенные подстроки как отдельные
                    i += candidate.length() - 1;
                }
            }
        }
        return result;
    }

    private static int firstBracketIndex(String text) {
        int a = text.indexOf('{');
        int b = text.indexOf('[');
        if (a == -1) return b;
        if (b == -1) return a;
        return Math.min(a, b);
    }

    private static String extractByBalancingFromIndex(String text, int start) {
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
        // не нашли закрывающую скобку
        return "";
    }

    /**
     * Проверяет, является ли строка валидным JSON.
     *
     * @param json      строка
     * @param strictTry если true — использовать строгий mapper, иначе permissive
     * @return true если парсится
     */
    private static boolean isValidJson(String json, boolean strictTry) {
        if (json == null || json.isBlank()) return false;
        try {
            if (strictTry) {
                STRICT_MAPPER.readTree(json);
            } else {
                PERMISSIVE_MAPPER.readTree(json);
            }
            return true;
        } catch (Exception ex) {
            // невалидный json для текущего маппера
            return false;
        }
    }
}
