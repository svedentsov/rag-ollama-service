package com.example.ragollama.agent.ux.domain;

import com.example.ragollama.agent.AgentContext;
import com.example.ragollama.agent.AgentResult;
import com.example.ragollama.agent.ToolAgent;
import com.example.ragollama.agent.ux.model.AgentCommand;
import com.example.ragollama.agent.ux.model.SimulationReport;
import com.example.ragollama.agent.ux.tools.PlaywrightActionService;
import com.example.ragollama.shared.exception.ProcessingException;
import com.example.ragollama.shared.llm.LlmClient;
import com.example.ragollama.shared.llm.ModelCapability;
import com.example.ragollama.shared.prompts.PromptService;
import com.example.ragollama.shared.util.JsonExtractorUtil;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;
import reactor.core.publisher.MonoSink;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Автономный AI-агент, который симулирует поведение пользователя в веб-браузере.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class UserBehaviorSimulatorAgent implements ToolAgent {

    private final PlaywrightActionService playwright;
    private final LlmClient llmClient;
    private final PromptService promptService;
    private final ObjectMapper objectMapper;
    private static final int MAX_STEPS = 10;
    private final JsonExtractorUtil jsonExtractorUtil;

    @Override
    public String getName() {
        return "user-behavior-simulator";
    }

    @Override
    public String getDescription() {
        return "Симулирует E2E-сценарий, управляя браузером на основе высокоуровневой цели.";
    }

    @Override
    public boolean canHandle(AgentContext context) {
        return context.payload().containsKey("startUrl") && context.payload().containsKey("goal");
    }

    @Override
    public Mono<AgentResult> execute(AgentContext context) {
        String startUrl = (String) context.payload().get("startUrl");
        String goal = (String) context.payload().get("goal");
        List<AgentCommand> history = new ArrayList<>();

        playwright.startSession();
        playwright.goTo(startUrl);

        return Mono.<AgentResult>create(sink -> runNextStep(goal, history, 0, sink))
                .doFinally(signalType -> playwright.closeSession());
    }

    private void runNextStep(String goal, List<AgentCommand> history, int step, MonoSink<AgentResult> sink) {
        if (step >= MAX_STEPS) {
            finishSimulation(goal, history, "FAILURE", "Достигнут максимальный лимит шагов.", sink);
            return;
        }

        String dom = playwright.getDom();
        String promptString = promptService.render("userBehaviorSimulatorPrompt", Map.of(
                "goal", goal, "history", history, "current_dom", dom
        ));

        llmClient.callChat(new Prompt(promptString), ModelCapability.BALANCED)
                .subscribe(
                        llmResponse -> {
                            try {
                                AgentCommand command = parseLlmResponse(llmResponse);
                                history.add(command);
                                executeCommand(command);

                                if ("finish".equalsIgnoreCase(command.command())) {
                                    finishSimulation(goal, history, "SUCCESS", command.thought(), sink);
                                } else {
                                    runNextStep(goal, history, step + 1, sink);
                                }
                            } catch (Exception e) {
                                log.error("Ошибка на шаге {} симуляции: {}", step, e.getMessage(), e);
                                finishSimulation(goal, history, "FAILURE", "Ошибка выполнения: " + e.getMessage(), sink);
                            }
                        },
                        error -> {
                            log.error("Ошибка при вызове LLM на шаге {}: {}", step, error.getMessage(), error);
                            finishSimulation(goal, history, "FAILURE", "Ошибка AI: " + error.getMessage(), sink);
                        }
                );
    }

    private void executeCommand(AgentCommand command) {
        Map<String, Object> args = command.arguments();
        switch (command.command().toLowerCase()) {
            case "click" -> playwright.click((String) args.get("selector"));
            case "fill" -> playwright.fill((String) args.get("selector"), (String) args.get("text"));
            case "assert" -> playwright.assertText((String) args.get("selector"), (String) args.get("text"));
            case "finish" -> {
            }
            default -> throw new IllegalArgumentException("Неизвестная команда: " + command.command());
        }
    }

    private void finishSimulation(String goal, List<AgentCommand> history, String outcome, String summary, MonoSink<AgentResult> sink) {
        SimulationReport report = new SimulationReport(goal, history, outcome, summary);
        AgentResult result = new AgentResult(getName(), "SUCCESS".equals(outcome) ? AgentResult.Status.SUCCESS : AgentResult.Status.FAILURE, summary, Map.of("simulationReport", report));
        sink.success(result);
    }

    private AgentCommand parseLlmResponse(String jsonResponse) {
        try {
            String cleanedJson = jsonExtractorUtil.extractJsonBlock(jsonResponse);
            return objectMapper.readValue(cleanedJson, AgentCommand.class);
        } catch (JsonProcessingException e) {
            throw new ProcessingException("User Simulator LLM вернул невалидный JSON.", e);
        }
    }
}
