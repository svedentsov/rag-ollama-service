package com.example.ragollama.agent;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Сервис-оркестратор, который управляет выполнением конвейеров AI-агентов.
 * <p>
 * Эта версия реализует паттерн "Стратегия". Оркестратор автоматически обнаруживает
 * все бины, реализующие {@link AgentPipeline}, и предоставляет API для их
 * асинхронного запуска по имени. Он не содержит логики композиции конвейеров,
 * а лишь делегирует выполнение соответствующей "стратегии" (реализации конвейера).
 *
 * @see AgentPipeline
 */
@Slf4j
@Service
public class AgentOrchestratorService {

    private final Map<String, AgentPipeline> pipelines;

    /**
     * Конструктор, который автоматически обнаруживает все доступные конвейеры
     * (бины, реализующие {@link AgentPipeline}) и инициализирует их реестр.
     *
     * @param pipelineProvider Провайдер для ленивого получения списка бинов конвейеров.
     */
    public AgentOrchestratorService(ObjectProvider<List<AgentPipeline>> pipelineProvider) {
        List<AgentPipeline> pipelineBeans = pipelineProvider.getIfAvailable(Collections::emptyList);
        this.pipelines = pipelineBeans.stream()
                .collect(Collectors.toMap(AgentPipeline::getName, Function.identity()));
        log.info("AgentOrchestratorService инициализирован. Зарегистрировано {} конвейеров: {}",
                pipelines.size(), pipelines.keySet());
    }

    /**
     * Предоставляет публичный доступ к списку имен всех
     * зарегистрированных конвейеров.
     *
     * @return Неизменяемое множество имен конвейеров.
     */
    public Set<String> getAvailablePipelines() {
        return Collections.unmodifiableSet(pipelines.keySet());
    }

    /**
     * Асинхронно запускает конвейер по его имени.
     * <p>
     * Метод находит соответствующую стратегию {@link AgentPipeline}, получает от нее
     * упорядоченный список агентов и последовательно выполняет каждого из них,
     * передавая и обогащая {@link AgentContext} на каждом шаге.
     *
     * @param pipelineName   Уникальное имя конвейера.
     * @param initialContext Начальный контекст с входными данными.
     * @return {@link CompletableFuture} с полным списком результатов от всех
     * агентов, выполненных в конвейере.
     * @throws IllegalArgumentException если конвейер с указанным именем не найден.
     */
    public CompletableFuture<List<AgentResult>> invokePipeline(String pipelineName, AgentContext initialContext) {
        AgentPipeline pipeline = pipelines.get(pipelineName);
        if (pipeline == null) {
            log.error("Попытка вызова несуществующего конвейера: '{}'. Доступные конвейеры: {}",
                    pipelineName, pipelines.keySet());
            return CompletableFuture.failedFuture(
                    new IllegalArgumentException("Конвейер с именем '" + pipelineName + "' не найден.")
            );
        }

        List<QaAgent> agentsInPipeline = pipeline.getAgents();
        log.info("Запуск конвейера '{}' с {} агентами.", pipelineName, agentsInPipeline.size());

        CompletableFuture<PipelineExecutionState> executionChain = CompletableFuture.completedFuture(
                new PipelineExecutionState(initialContext, List.of())
        );

        for (QaAgent agent : agentsInPipeline) {
            executionChain = executionChain.thenCompose(state -> {
                log.debug("Выполнение агента '{}' в конвейере '{}'", agent.getName(), pipelineName);
                if (!agent.canHandle(state.currentContext)) {
                    log.warn("Агент '{}' пропущен, так как не может обработать текущий контекст.", agent.getName());
                    return CompletableFuture.completedFuture(state);
                }
                return agent.execute(state.currentContext)
                        .thenApply(state::addResult);
            });
        }

        return executionChain.thenApply(PipelineExecutionState::getResults);
    }

    /**
     * Внутренний вспомогательный класс для хранения текущего состояния
     * выполнения конвейера.
     * <p>
     * Он является неизменяемым (immutable) и на каждом шаге создает новый
     * экземпляр с обновленным контекстом и списком результатов, что обеспечивает
     * потокобезопасность и предсказуемость.
     */
    private static class PipelineExecutionState {
        private final AgentContext currentContext;
        private final List<AgentResult> results;

        PipelineExecutionState(AgentContext currentContext, List<AgentResult> results) {
            this.currentContext = currentContext;
            this.results = new java.util.ArrayList<>(results);
        }

        /**
         * Добавляет новый результат и обогащает контекст для следующего шага.
         *
         * @param newResult Результат работы предыдущего агента.
         * @return Новый, обновленный экземпляр {@link PipelineExecutionState}.
         */
        public PipelineExecutionState addResult(AgentResult newResult) {
            List<AgentResult> newResults = new java.util.ArrayList<>(this.results);
            newResults.add(newResult);
            Map<String, Object> newPayload = new java.util.HashMap<>(this.currentContext.payload());
            newPayload.putAll(newResult.details());
            return new PipelineExecutionState(new AgentContext(newPayload), newResults);
        }

        public List<AgentResult> getResults() {
            return results;
        }
    }
}
