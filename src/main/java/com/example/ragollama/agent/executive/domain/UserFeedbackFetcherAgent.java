package com.example.ragollama.agent.executive.domain;

import com.example.ragollama.agent.AgentContext;
import com.example.ragollama.agent.AgentResult;
import com.example.ragollama.agent.ToolAgent;
import com.example.ragollama.monitoring.KnowledgeGapRepository;
import com.example.ragollama.monitoring.domain.FeedbackLogRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Агент-инструмент для сбора "сырой" обратной связи от пользователей.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class UserFeedbackFetcherAgent implements ToolAgent {

    private final FeedbackLogRepository feedbackLogRepository;
    private final KnowledgeGapRepository knowledgeGapRepository;

    @Override
    public String getName() {
        return "user-feedback-fetcher";
    }

    @Override
    public String getDescription() {
        return "Собирает негативный фидбэк и 'пробелы в знаниях' из БД за период.";
    }

    @Override
    public boolean canHandle(AgentContext context) {
        return context.payload().containsKey("analysisPeriodDays");
    }

    @Override
    public CompletableFuture<AgentResult> execute(AgentContext context) {
        return CompletableFuture.supplyAsync(() -> {
            int days = (int) context.payload().get("analysisPeriodDays");
            log.info("Сбор фидбэка за последние {} дней...", days);

            List<String> negativeFeedback = feedbackLogRepository.findAll(PageRequest.of(0, 100)).stream()
                    .filter(fb -> !fb.getIsHelpful() && fb.getUserComment() != null)
                    .map(fb -> fb.getUserComment())
                    .toList();

            List<String> knowledgeGaps = knowledgeGapRepository.findAll(PageRequest.of(0, 100)).stream()
                    .map(com.example.ragollama.monitoring.model.KnowledgeGap::getQueryText)
                    .toList();

            List<String> allFeedback = Stream.concat(negativeFeedback.stream(), knowledgeGaps.stream()).collect(Collectors.toList());

            return new AgentResult(getName(), AgentResult.Status.SUCCESS, "Собрано " + allFeedback.size() + " записей обратной связи.", Map.of("rawFeedback", allFeedback));
        });
    }
}
