package com.example.ragollama.agent.copilot;

import com.example.ragollama.agent.AgentResult;
import com.example.ragollama.agent.copilot.api.dto.CopilotRequest;
import com.example.ragollama.agent.copilot.api.dto.CopilotResponse;
import com.example.ragollama.agent.dynamic.DynamicPipelineExecutionService;
import com.example.ragollama.agent.dynamic.PlanningAgentService;
import com.example.ragollama.shared.llm.LlmClient;
import com.example.ragollama.shared.llm.ModelCapability;
import com.example.ragollama.shared.prompts.PromptService;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Сервис-оркестратор для QA Copilot.
 * <p>
 * Является stateful-компонентом, который управляет диалоговыми сессиями,
 * вызывает stateless-планировщик и исполнитель, и суммирует результаты
 * для предоставления человекочитаемого ответа.
 * <p>
 * В этой версии удалена вся специализированная логика по обработке XAI-запросов.
 * Теперь любой запрос, включая просьбу об объяснении, обрабатывается
 * универсальным конвейером "планирование -> выполнение".
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class QaCopilotService {

    private final CopilotSessionService sessionService;
    private final PlanningAgentService planningAgentService;
    private final DynamicPipelineExecutionService executionService;
    private final LlmClient llmClient;
    private final PromptService promptService;
    private final ObjectMapper objectMapper;

    /**
     * Обрабатывает сообщение от пользователя в рамках сессии.
     *
     * @param request DTO с запросом от пользователя.
     * @return {@link Mono} с ответом копайлота.
     */
    public Mono<CopilotResponse> processUserMessage(CopilotRequest request) {
        UUID sessionId = sessionService.getOrCreateSessionId(request.sessionId());
        CopilotSession session = sessionService.getSession(sessionId);

        // Дополняем контекст сессии последним результатом для планировщика
        Map<String, Object> planningContext = new HashMap<>(session.getAccumulatedContext());
        if (session.getLastAgentResult() != null) {
            planningContext.put("lastAgentResult", session.getLastAgentResult());
        }

        session.addMessage(new CopilotSession.ChatMessage(CopilotSession.Role.USER, request.message()));

        return planningAgentService.createPlan(request.message(), planningContext)
                .flatMap(plan -> executionService.executePlan(plan, session.toAgentContext(), sessionId))
                .flatMap(results -> {
                    Mono<String> summaryMono = summarizeResults(request.message(), results);
                    return summaryMono.map(summary -> {
                        session.addMessage(new CopilotSession.ChatMessage(CopilotSession.Role.ASSISTANT, summary));
                        results.forEach(result -> session.updateContext(result.details()));
                        if (!results.isEmpty()) {
                            // Сохраняем не весь AgentResult, а только его "полезную нагрузку"
                            session.setLastAgentResult(results.getLast().details());
                        }
                        sessionService.updateSession(sessionId, session);
                        return summary;
                    });
                })
                .map(summary -> new CopilotResponse(summary, sessionId));
    }

    private Mono<String> summarizeResults(String userQuery, List<AgentResult> results) {
        if (results.isEmpty()) {
            return Mono.just("Я не смог выполнить эту задачу, так как не нашел подходящих инструментов или план выполнения был пуст.");
        }

        AgentResult lastResult = results.get(results.size() - 1);
        if ("human-in-the-loop-gate-agent".equals(lastResult.agentName())) {
            return Mono.just("Для продолжения выполнения задачи требуется ваше утверждение. " +
                    "ID операции для утверждения: `" + lastResult.details().get("executionId") + "`.\n\n" +
                    "Вы можете утвердить, выполнив запрос: `POST /api/v1/reviews/" + lastResult.details().get("executionId") + "/approve`");
        }

        try {
            String resultsAsJson = objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(results);
            String promptString = promptService.render("copilotResultSummarizerPrompt", Map.of(
                    "userQuery", userQuery,
                    "resultsJson", resultsAsJson
            ));
            return llmClient.callChat(new Prompt(promptString), ModelCapability.BALANCED)
                    .map(tuple -> tuple.getT1());
        } catch (JsonProcessingException e) {
            log.error("Не удалось сериализовать результаты агентов в JSON", e);
            return Mono.just("Произошла внутренняя ошибка при форматировании ответа.");
        }
    }
}
