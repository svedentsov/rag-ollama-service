package com.example.ragollama.agent.copilot;

import com.example.ragollama.agent.AgentContext;
import com.example.ragollama.agent.AgentResult;
import com.example.ragollama.agent.copilot.api.dto.CopilotRequest;
import com.example.ragollama.agent.copilot.api.dto.CopilotResponse;
import com.example.ragollama.agent.dynamic.DynamicPipelineExecutionService;
import com.example.ragollama.agent.dynamic.PlanningAgentService;
import com.example.ragollama.agent.xai.domain.ExplainerAgent;
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
 * Эта версия также включает логику для распознавания запросов на объяснение
 * (XAI) и их делегирования специализированному {@link ExplainerAgent}.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class QaCopilotService {

    private final CopilotSessionService sessionService;
    private final PlanningAgentService planningAgentService;
    private final DynamicPipelineExecutionService executionService;
    private final ExplainerAgent explainerAgent;
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

        session.addMessage(new CopilotSession.ChatMessage(CopilotSession.Role.USER, request.message()));

        if (isExplanationRequest(request.message())) {
            return handleExplanationRequest(request, session, sessionId);
        }

        return planningAgentService.createPlan(request.message(), session.getAccumulatedContext())
                .flatMap(plan -> executionService.executePlan(plan, session.toAgentContext(), sessionId))
                .flatMap(results -> {
                    Mono<String> summaryMono = summarizeResults(request.message(), results);
                    return summaryMono.map(summary -> {
                        session.addMessage(new CopilotSession.ChatMessage(CopilotSession.Role.ASSISTANT, summary));
                        results.forEach(result -> session.updateContext(result.details()));
                        if (!results.isEmpty()) {
                            session.setLastAgentResult(results.get(results.size() - 1));
                        }
                        sessionService.updateSession(sessionId, session);
                        return summary;
                    });
                })
                .map(summary -> new CopilotResponse(summary, sessionId));
    }

    private Mono<CopilotResponse> handleExplanationRequest(CopilotRequest request, CopilotSession session, UUID sessionId) {
        log.info("Обнаружен запрос на объяснение в сессии {}.", sessionId);
        AgentResult lastResult = session.getLastAgentResult();

        if (lastResult == null) {
            String response = "Мне нечего объяснять, так как в этой сессии еще не было выполнено ни одной задачи.";
            session.addMessage(new CopilotSession.ChatMessage(CopilotSession.Role.ASSISTANT, response));
            sessionService.updateSession(sessionId, session);
            return Mono.just(new CopilotResponse(response, sessionId));
        }

        AgentContext explainerContext = new AgentContext(Map.of(
                "userQuestion", request.message(),
                "technicalContext", lastResult
        ));

        return explainerAgent.execute(explainerContext)
                .map(explanationResult -> (String) explanationResult.details().get("explanation"))
                .doOnNext(explanation -> {
                    session.addMessage(new CopilotSession.ChatMessage(CopilotSession.Role.ASSISTANT, explanation));
                    sessionService.updateSession(sessionId, session);
                })
                .map(explanation -> new CopilotResponse(explanation, sessionId));
    }

    private boolean isExplanationRequest(String message) {
        String lowerCaseMsg = message.toLowerCase().trim();
        return lowerCaseMsg.startsWith("объясни") || lowerCaseMsg.startsWith("почему") ||
                lowerCaseMsg.startsWith("растолкуй") || lowerCaseMsg.contains("что это значит");
    }

    private Mono<String> summarizeResults(String userQuery, List<AgentResult> results) {
        if (results.isEmpty()) {
            return Mono.just("Я не смог выполнить эту задачу, так как не нашел подходящих инструментов или план выполнения был пуст.");
        }

        AgentResult lastResult = results.get(results.size() - 1);
        if ("human-in-the-loop-gate".equals(lastResult.agentName())) {
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
            return llmClient.callChat(new Prompt(promptString), ModelCapability.BALANCED);
        } catch (JsonProcessingException e) {
            log.error("Не удалось сериализовать результаты агентов в JSON", e);
            return Mono.just("Произошла внутренняя ошибка при форматировании ответа.");
        }
    }
}
