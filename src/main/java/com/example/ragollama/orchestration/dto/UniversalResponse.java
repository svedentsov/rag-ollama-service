package com.example.ragollama.orchestration.dto;

import com.example.ragollama.agent.buganalysis.model.BugAnalysisReport;
import com.example.ragollama.agent.codegeneration.api.dto.CodeGenerationResponse;
import com.example.ragollama.rag.api.dto.StreamingResponsePart;
import com.example.ragollama.rag.domain.model.QueryFormationStep;
import com.example.ragollama.rag.domain.model.SourceCitation;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;
import java.util.UUID;

/**
 * Запечатанный (sealed) интерфейс для представления различных частей
 * унифицированного потокового ответа от оркестратора.
 * <p>
 * В эту версию добавлен новый тип события `ThinkingThought` для трансляции
 * прогресса выполнения динамических планов.
 */
@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, property = "type")
@JsonSubTypes({
        @JsonSubTypes.Type(value = UniversalResponse.TaskStarted.class, name = "task_started"),
        @JsonSubTypes.Type(value = UniversalResponse.StatusUpdate.class, name = "status_update"),
        @JsonSubTypes.Type(value = UniversalResponse.ThinkingThought.class, name = "thinking_thought"),
        @JsonSubTypes.Type(value = UniversalResponse.Content.class, name = "content"),
        @JsonSubTypes.Type(value = UniversalResponse.Sources.class, name = "sources"),
        @JsonSubTypes.Type(value = UniversalResponse.Code.class, name = "code"),
        @JsonSubTypes.Type(value = UniversalResponse.Done.class, name = "done"),
        @JsonSubTypes.Type(value = UniversalResponse.Error.class, name = "error"),
        @JsonSubTypes.Type(value = UniversalResponse.BugAnalysis.class, name = "bugAnalysis")
})
@JsonInclude(JsonInclude.Include.NON_NULL)
public sealed interface UniversalResponse {

    /**
     * Сигнал о старте задачи, содержит ее ID для отслеживания.
     *
     * @param taskId Уникальный идентификатор задачи.
     */
    @Schema(description = "Сигнал о старте задачи, содержит ее ID")
    record TaskStarted(UUID taskId) implements UniversalResponse {
    }

    /**
     * Промежуточное обновление статуса выполнения задачи на бэкенде.
     *
     * @param text Текст статуса (например, "Ищу информацию...").
     */
    @Schema(description = "Обновление статуса выполнения задачи на бэкенде")
    record StatusUpdate(String text) implements UniversalResponse {
    }

    /**
     * Представляет один шаг ("мысль") в процессе выполнения AI-агентом своего плана.
     *
     * @param stepName Имя выполняемого шага/агента.
     * @param status   Текущий статус шага.
     */
    @Schema(description = "Шаг в процессе 'мышления' AI-агента")
    record ThinkingThought(String stepName, ThoughtStatus status) implements UniversalResponse {
    }

    /**
     * Перечисление статусов для шага "мышления".
     */
    enum ThoughtStatus {
        RUNNING, COMPLETED
    }

    /**
     * Часть сгенерированного текстового контента ответа.
     *
     * @param text Фрагмент текста.
     */
    @Schema(description = "Часть сгенерированного текстового контента")
    record Content(String text) implements UniversalResponse {
    }

    /**
     * Список структурированных цитат (источников), использованных для RAG-ответа.
     * Отправляется единым блоком.
     *
     * @param sources               Список цитат.
     * @param queryFormationHistory История обработки запроса.
     * @param finalPrompt           Полный текст промпта, отправленного в LLM.
     */
    @Schema(description = "Список структурированных цитат, использованных для RAG-ответа")
    record Sources(List<SourceCitation> sources, List<QueryFormationStep> queryFormationHistory,
                   String finalPrompt) implements UniversalResponse {
    }

    /**
     * Результат работы агента кодогенерации. Отправляется единым блоком.
     *
     * @param generatedCode Сгенерированный код.
     * @param language      Язык программирования.
     * @param finalPrompt   Полный текст промпта, отправленного в LLM.
     */
    @Schema(description = "Результат работы агента кодогенерации")
    record Code(String generatedCode, String language, String finalPrompt) implements UniversalResponse {
    }

    /**
     * Результат работы агента анализа багов. Отправляется единым блоком.
     *
     * @param analysis Полный отчет об анализе.
     */
    @Schema(description = "Результат работы агента анализа багов")
    record BugAnalysis(BugAnalysisReport analysis) implements UniversalResponse {
    }

    /**
     * Сигнал об успешном завершении потока и финализации (сохранении) ответа на сервере.
     *
     * @param message Сообщение о статусе завершения.
     */
    @Schema(description = "Сигнал об успешном завершении и сохранении ответа")
    record Done(String message) implements UniversalResponse {
    }

    /**
     * Сообщение об ошибке, возникшей во время обработки потока.
     *
     * @param message Детальное описание ошибки.
     */
    @Schema(description = "Сообщение об ошибке в потоке")
    record Error(String message) implements UniversalResponse {
    }


    /**
     * Фабричный метод для преобразования {@link StreamingResponsePart} от RAG-сервиса.
     *
     * @param part Часть ответа от RAG-сервиса.
     * @return Экземпляр UniversalResponse.
     */
    static UniversalResponse from(StreamingResponsePart part) {
        return switch (part) {
            case StreamingResponsePart.Content c -> new Content(c.text());
            case StreamingResponsePart.Sources s ->
                    new Sources(s.sources(), s.queryFormationHistory(), s.finalPrompt());
            case StreamingResponsePart.Done d -> new Done(d.message());
            case StreamingResponsePart.Error e -> new Error(e.message());
        };
    }

    /**
     * Фабричный метод для преобразования текстового чанка от Чат-сервиса.
     *
     * @param textChunk Фрагмент текста.
     * @return Экземпляр UniversalResponse.
     */
    static UniversalResponse from(String textChunk) {
        return new Content(textChunk);
    }

    /**
     * Фабричный метод для преобразования ответа от сервиса кодогенерации.
     *
     * @param response Ответ от сервиса.
     * @return Экземпляр UniversalResponse.
     */
    static UniversalResponse from(CodeGenerationResponse response) {
        return new Code(response.generatedCode(), response.language(), response.finalPrompt());
    }

    /**
     * Фабричный метод для преобразования ответа от сервиса анализа багов.
     *
     * @param response Ответ от сервиса.
     * @return Экземпляр UniversalResponse.
     */
    static UniversalResponse from(BugAnalysisReport response) {
        return new BugAnalysis(response);
    }

    /**
     * Фабричный метод для преобразования полного синхронного ответа в одну потоковую часть.
     *
     * @param syncResponse Синхронный ответ.
     * @return Экземпляр UniversalResponse.
     */
    static UniversalResponse from(UniversalSyncResponse syncResponse) {
        if (syncResponse.bugAnalysisResponse() != null) {
            return new BugAnalysis(syncResponse.bugAnalysisResponse());
        }
        if (syncResponse.generatedCode() != null) {
            return new Code(syncResponse.generatedCode(), syncResponse.language(), syncResponse.finalPrompt());
        }
        return new Content(syncResponse.answer() != null ? syncResponse.answer() : "");
    }
}
