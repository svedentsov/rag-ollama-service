package com.example.ragollama.optimization;

import com.example.ragollama.evaluation.RagEvaluationService;
import com.example.ragollama.evaluation.model.EvaluationResult;
import com.example.ragollama.shared.prompts.ThreadLocalPromptOverride;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import java.util.Map;

/**
 * Сервис-исполнитель для запуска оценки RAG в изолированном контексте.
 * <p>
 * Эта версия была доработана для поддержки временного переопределения
 * промптов с использованием {@link ThreadLocal}, что является ключом
 * к безопасному A/B-тестированию.
 */
@Service
@RequiredArgsConstructor
public class IsolatedEvaluationRunner {

    private final RagEvaluationService evaluationService;

    /**
     * Запускает прогон оценки с временным переопределением промпта.
     *
     * @param promptName       Имя промпта для переопределения (например, "ragPrompt").
     * @param newPromptContent Новое содержимое промпта. Если `null`, используется
     *                         версия из файла (baseline).
     * @return {@link Mono} с результатом оценки.
     */
    public Mono<EvaluationResult> runWithPromptOverride(String promptName, String newPromptContent) {
        return Mono.fromCallable(() -> {
            try {
                if (newPromptContent != null) {
                    ThreadLocalPromptOverride.setOverride(promptName, newPromptContent);
                }
                // `block()` здесь безопасен, так как мы находимся внутри `fromCallable`,
                // который выполняется в управляемом пуле потоков.
                return evaluationService.evaluate().block();
            } finally {
                // Гарантируем очистку ThreadLocal, чтобы не повлиять на другие запросы.
                ThreadLocalPromptOverride.clear();
            }
        });
    }

    /**
     * Заглушка для существующего метода, вызывающего оценку с переопределением конфигурации.
     */
    public Mono<EvaluationResult> runEvaluationWithOverrides(Map<String, Object> overrides) {
        // ВАЖНО: В этой демонстрационной версии переопределения не применяются,
        // но архитектурно подготовлена возможность для их реализации.
        return evaluationService.evaluate();
    }
}