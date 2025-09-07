package com.example.ragollama.shared.util;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.json.JsonMapper;
import lombok.experimental.UtilityClass;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;

import java.util.ArrayList;
import java.util.List;

/**
 * Утилитарный класс для надежного извлечения JSON-блоков из текста,
 * который может содержать посторонние символы или markdown-разметку,
 * что часто встречается в ответах от LLM.
 * <p>
 * Этот класс реализует многоступенчатую, отказоустойчивую стратегию
 * для поиска и валидации JSON-объектов или массивов внутри произвольной строки.
 * Он является ключевым компонентом для обеспечения надежности (robustness)
 * при взаимодействии с непредсказуемым выводом языковых моделей.
 */
@Slf4j
@UtilityClass
public class JsonExtractorUtil {

    /**
     * Строгий парсер, соответствующий стандарту JSON.
     * Инициализирован с использованием современного builder API.
     */
    private static final ObjectMapper STRICT_MAPPER = JsonMapper.builder().build();

    /**
     * "Снисходительный" парсер, который допускает некоторые отклонения от
     * стандарта, такие как комментарии, висячие запятые и т.д.
     * Используется как fallback-стратегия и инициализирован через builder.
     */
    private static final ObjectMapper PERMISSIVE_MAPPER = JsonMapper.builder()
            .enable(JsonParser.Feature.ALLOW_COMMENTS)
            .enable(JsonParser.Feature.ALLOW_SINGLE_QUOTES)
            .enable(JsonParser.Feature.ALLOW_UNQUOTED_FIELD_NAMES)
            .enable(JsonParser.Feature.ALLOW_TRAILING_COMMA)
            .build();

    /**
     * Извлекает первый валидный JSON-блок (объект или массив) из текста.
     * <p>
     * Метод последовательно применяет несколько стратегий:
     * <ol>
     *   <li>Поиск JSON внутри markdown-блока (```json ... ```).</li>
     *   <li>Поиск всех сбалансированных блоков `{...}` или `[...]` и их проверка.</li>
     *   <li>Fallback-поиск от первой найденной открывающей скобки.</li>
     * </ol>
     * <p>
     * Каждая найденная строка-кандидат проверяется сначала строгим, а затем
     * "снисходительным" парсером для максимальной отказоустойчивости.
     *
     * @param text Ответ от LLM, потенциально содержащий "мусор".
     * @return Строка с валидным JSON или пустая строка, если JSON не найден.
     */
    public static String extractJsonBlock(String text) {
        if (text == null || text.isBlank()) {
            return "";
        }

        String fromMarkdown = extractFromMarkdown(text);
        if (!fromMarkdown.isEmpty()) {
            if (isValidJson(fromMarkdown, true)) return fromMarkdown;
            if (isValidJson(fromMarkdown, false)) return fromMarkdown;
        }

        List<String> candidates = extractAllByBalancingBrackets(text);
        for (String c : candidates) {
            if (isValidJson(c, true)) return c;
        }
        for (String c : candidates) {
            if (isValidJson(c, false)) return c;
        }

        int firstBrace = firstBracketIndex(text);
        if (firstBrace != -1) {
            String fallback = extractByBalancingFromIndex(text, firstBrace);
            if (!fallback.isEmpty()) {
                if (isValidJson(fallback, true)) return fallback;
                if (isValidJson(fallback, false)) return fallback;
            }
        }

        log.warn("JsonExtractorUtil: не удалось извлечь валидный JSON из ответа LLM. Отрезок (макс 1000): {}",
                StringUtils.left(text, 1000));
        return "";
    }

    /**
     * Извлекает содержимое из markdown-блока для JSON.
     *
     * @param text Исходный текст.
     * @return Содержимое блока или пустая строка.
     */
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

    /**
     * Находит все сбалансированные блоки скобок в тексте.
     *
     * @param text Исходный текст.
     * @return Список строк, каждая из которых является сбалансированным блоком.
     */
    private static List<String> extractAllByBalancingBrackets(String text) {
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

    /**
     * Находит индекс первой открывающей скобки '{' или '['.
     *
     * @param text Исходный текст.
     * @return Индекс или -1, если скобки не найдены.
     */
    private static int firstBracketIndex(String text) {
        int braceIndex = text.indexOf('{');
        int bracketIndex = text.indexOf('[');
        if (braceIndex == -1) return bracketIndex;
        if (bracketIndex == -1) return braceIndex;
        return Math.min(braceIndex, bracketIndex);
    }

    /**
     * Извлекает сбалансированный блок, начиная с указанного индекса.
     *
     * @param text  Исходный текст.
     * @param start Индекс, с которого начинается открывающая скобка.
     * @return Извлеченный блок или пустая строка.
     */
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
        return "";
    }

    /**
     * Проверяет, является ли строка валидным JSON.
     *
     * @param json      Строка-кандидат.
     * @param strictTry Если {@code true}, используется строгий парсер,
     *                  иначе — "снисходительный".
     * @return {@code true}, если строка является валидным JSON.
     */
    private static boolean isValidJson(String json, boolean strictTry) {
        if (json == null || json.isBlank()) return false;
        try {
            (strictTry ? STRICT_MAPPER : PERMISSIVE_MAPPER).readTree(json);
            return true;
        } catch (Exception ex) {
            return false;
        }
    }
}
