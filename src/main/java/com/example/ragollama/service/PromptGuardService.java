package com.example.ragollama.service;

import com.example.ragollama.exception.PromptInjectionException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.regex.Pattern;

/**
 * Сервис для защиты от атак типа "Prompt Injection".
 * <p>
 * Этот сервис предоставляет базовые механизмы для обнаружения и предотвращения
 * попыток злонамеренного воздействия на языковую модель через пользовательский ввод.
 */
@Service
public class PromptGuardService {

    private static final Logger log = LoggerFactory.getLogger(PromptGuardService.class);

    /**
     * Список регулярных выражений, соответствующих известным паттернам атак.
     * Эти паттерны нацелены на обнаружение попыток:
     * - Игнорировать предыдущие инструкции.
     * - Раскрыть системный промпт или конфигурацию.
     * - Выполнить нежелательные действия.
     */
    private static final List<Pattern> INJECTION_PATTERNS = List.of(
            Pattern.compile("ignore previous instructions", Pattern.CASE_INSENSITIVE),
            Pattern.compile("ignore all prior instructions", Pattern.CASE_INSENSITIVE),
            Pattern.compile("forget all your previous instructions", Pattern.CASE_INSENSITIVE),
            Pattern.compile("reveal your system prompt", Pattern.CASE_INSENSITIVE),
            Pattern.compile("what are your instructions", Pattern.CASE_INSENSITIVE),
            Pattern.compile("translate the text above", Pattern.CASE_INSENSITIVE), // Попытка обойти контекст
            Pattern.compile("repeat the words above", Pattern.CASE_INSENSITIVE) // DAN (Do Anything Now) атаки
    );

    /**
     * Проверяет входной промпт на наличие потенциальных угроз.
     * <p>
     * Метод итерируется по списку известных вредоносных паттернов и, в случае
     * совпадения, выбрасывает исключение {@link PromptInjectionException}.
     *
     * @param prompt Пользовательский ввод, который необходимо проверить.
     * @throws PromptInjectionException если во вводе обнаружена потенциальная угроза.
     */
    public void checkForInjection(String prompt) {
        if (prompt == null || prompt.isBlank()) {
            return;
        }

        for (Pattern pattern : INJECTION_PATTERNS) {
            if (pattern.matcher(prompt).find()) {
                log.warn("Potential prompt injection detected. Pattern: '{}', Prompt: '{}'", pattern.pattern(), prompt);
                throw new PromptInjectionException(
                        "Обнаружена потенциальная атака 'Prompt Injection'. Ваш запрос был заблокирован."
                );
            }
        }
    }
}
