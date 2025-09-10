package com.example.ragollama.rag.pipeline;

import com.example.ragollama.rag.api.dto.StreamingResponsePart;
import com.example.ragollama.rag.domain.model.RagAnswer;
import com.example.ragollama.rag.pipeline.steps.GenerationStep;
import com.example.ragollama.rag.postprocessing.RagPostProcessingOrchestrator;
import com.example.ragollama.rag.postprocessing.RagProcessingContext;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.ai.chat.messages.Message;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

/**
 * Оркестратор RAG-конвейера, построенного на паттерне "Pipeline Step".
 * <p> Этот класс является центральным ядром RAG-системы. Он отвечает за последовательное
 * выполнение всех шагов конвейера, начиная от обработки входящего запроса и
 * заканчивая генерацией финального ответа.
 * <p> <b>Ключевое архитектурное решение:</b> Для устранения логического дедлока,
 * выполнение конвейера разделено на два этапа:
 * <ol>
 *     <li><b>Пред-генерационный этап (Pre-Generation):</b> Выполняются все шаги,
 *     необходимые для подготовки контекста (охрана промпта, обработка запроса,
 *     извлечение, переранжирование, сборка промпта).</li>
 *     <li><b>Этап Генерации (Generation):</b> На основе подготовленного контекста
 *     вызывается {@link GenerationStep} для генерации ответа от LLM.</li>
 * </ol>
 * Эта логика реализована как для стандартных асинхронных запросов, так и для
 * потоковых (SSE), обеспечивая корректную и надежную работу системы.
 *
 * @see RagPipelineStep
 * @see GenerationStep
 */
@Slf4j
@Service
public class RagPipelineOrchestrator {

    private final RagPostProcessingOrchestrator postProcessingOrchestrator;
    private final List<RagPipelineStep> preGenerationSteps;
    private final GenerationStep generationStep;

    /**
     * Конструктор, который автоматически обнаруживает все реализации {@link RagPipelineStep},
     * внедренные Spring, и разделяет их на пред-генерационные шаги и шаг генерации.
     *
     * @param allPipelineSteps           Список всех бинов, реализующих {@link RagPipelineStep},
     *                                   автоматически собранный Spring.
     * @param postProcessingOrchestrator Оркестратор для выполнения задач после генерации ответа.
     * @throws IllegalStateException если в списке шагов не найден ровно один экземпляр {@link GenerationStep}.
     */
    public RagPipelineOrchestrator(List<RagPipelineStep> allPipelineSteps, RagPostProcessingOrchestrator postProcessingOrchestrator) {
        this.postProcessingOrchestrator = postProcessingOrchestrator;
        // Находим и изолируем шаг генерации. Он является терминальным для основного конвейера.
        this.generationStep = allPipelineSteps.stream()
                .filter(GenerationStep.class::isInstance)
                .map(GenerationStep.class::cast)
                .findFirst()
                .orElseThrow(() -> new IllegalStateException("Конфигурация невалидна: в конвейере должен быть ровно один GenerationStep."));
        // Все остальные шаги считаются пред-генерационными.
        this.preGenerationSteps = allPipelineSteps.stream()
                .filter(step -> !(step instanceof GenerationStep))
                .toList();
        log.info("RagPipelineOrchestrator инициализирован. Пред-генерационных шагов: {}, Шаг генерации: {}.",
                preGenerationSteps.size(), generationStep.getClass().getSimpleName());
    }

    /**
     * Асинхронно выполняет полный RAG-конвейер для не-потокового запроса.
     * <p> Процесс выполнения:
     * <ol>
     *     <li>Запускает все пред-генерационные шаги для подготовки контекста и финального промпта.</li>
     *     <li>Вызывает шаг генерации для получения полного ответа от LLM.</li>
     *     <li>Инициирует асинхронную постобработку (логирование, метрики, верификация).</li>
     * </ol>
     *
     * @param query               Исходный запрос пользователя.
     * @param history             История чата для поддержания контекста.
     * @param topK                Количество извлекаемых документов.
     * @param similarityThreshold Порог схожести для векторного поиска.
     * @param sessionId           Уникальный идентификатор текущей сессии.
     * @return {@link CompletableFuture}, который по завершении будет содержать
     * финальный, готовый для пользователя {@link RagAnswer}.
     */
    public CompletableFuture<RagAnswer> queryAsync(String query, List<Message> history, int topK, double similarityThreshold, UUID sessionId) {
        final String requestId = MDC.get("requestId");
        RagFlowContext initialContext = new RagFlowContext(query, history, topK, similarityThreshold, sessionId);
        // 1. Выполняем все шаги ДО генерации
        Mono<RagFlowContext> contextReadyForGeneration = executePreGenerationPipeline(initialContext);
        // 2. Явно вызываем шаг генерации, когда предыдущие шаги завершены
        return contextReadyForGeneration
                .flatMap(generationStep::process) // Выполняем шаг генерации
                .map(finalContext -> {
                    // 3. Запускаем постобработку в режиме "fire-and-forget"
                    var processingContext = new RagProcessingContext(
                            requestId,
                            finalContext.originalQuery(),
                            finalContext.rerankedDocuments(),
                            finalContext.finalPrompt(),
                            finalContext.finalAnswer(),
                            finalContext.sessionId()
                    );
                    postProcessingOrchestrator.process(processingContext);
                    return finalContext.finalAnswer();
                })
                .toFuture();
    }

    /**
     * Выполняет RAG-конвейер в потоковом режиме (Server-Sent Events).
     *
     * @param query               Запрос пользователя.
     * @param history             История чата.
     * @param topK                Количество документов для извлечения.
     * @param similarityThreshold Порог схожести.
     * @param sessionId           ID сессии.
     * @return {@link Flux} со структурированными частями ответа {@link StreamingResponsePart}.
     */
    public Flux<StreamingResponsePart> queryStream(String query, List<Message> history, int topK, double similarityThreshold, UUID sessionId) {
        final String requestId = MDC.get("requestId");
        RagFlowContext initialContext = new RagFlowContext(query, history, topK, similarityThreshold, sessionId);

        // 1. Выполняем все шаги ДО генерации, чтобы получить готовый к генерации контекст
        Mono<RagFlowContext> contextReadyForGeneration = executePreGenerationPipeline(initialContext);

        // 2. На основе готового контекста запускаем потоковую генерацию
        return contextReadyForGeneration.flatMapMany(context -> {
            // Запускаем потоковую генерацию
            Flux<StreamingResponsePart> stream = generationStep.generateStructuredStream(context.finalPrompt(), context.rerankedDocuments());

            // 3. Собираем полный ответ в фоне для постобработки, не блокируя основной поток
            stream.collectList().subscribe(parts -> {
                String fullAnswer = parts.stream()
                        .filter(p -> p instanceof StreamingResponsePart.Content)
                        .map(p -> ((StreamingResponsePart.Content) p).text())
                        .collect(Collectors.joining());

                RagAnswer answerForPostProcessing = new RagAnswer(fullAnswer, new ArrayList<>());
                var processingContext = new RagProcessingContext(requestId, query, context.rerankedDocuments(),
                        context.finalPrompt(), answerForPostProcessing, sessionId);
                postProcessingOrchestrator.process(processingContext);
            });

            return stream;
        });
    }

    /**
     * Вспомогательный метод для последовательного выполнения пред-генерационных шагов конвейера.
     *
     * @param initialContext Начальный контекст.
     * @return {@link Mono} с финальным контекстом после выполнения всех пред-генерационных шагов.
     */
    private Mono<RagFlowContext> executePreGenerationPipeline(RagFlowContext initialContext) {
        // Используем Flux.reduce для последовательного асинхронного выполнения шагов
        return Flux.fromIterable(preGenerationSteps)
                .reduce(Mono.just(initialContext), (contextMono, step) -> contextMono.flatMap(step::process))
                .flatMap(mono -> mono); // Разворачиваем вложенный Mono
    }
}
