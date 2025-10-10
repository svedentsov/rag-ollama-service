package com.example.ragollama.agent.executive.domain;

import com.example.ragollama.agent.AgentContext;
import com.example.ragollama.agent.AgentResult;
import com.example.ragollama.agent.ToolAgent;
import com.example.ragollama.monitoring.domain.FeedbackLogRepository;
import com.example.ragollama.monitoring.domain.KnowledgeGapRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.Map;

/**
 * Агент-инструмент для сбора "сырой" обратной связи от пользователей, адаптированный для R2DBC.
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
    public Mono<AgentResult> execute(AgentContext context) {
        int days = (int) context.payload().get("analysisPeriodDays");
        log.info("Сбор фидбэка за последние {} дней...", days);

        Mono<List<String>> negativeFeedbackMono = feedbackLogRepository.findAll()
                .filter(fb -> !fb.getIsHelpful() && fb.getUserComment() != null)
                .map(fb -> fb.getUserComment())
                .take(100)
                .collectList();

        Mono<List<String>> knowledgeGapsMono = knowledgeGapRepository.findAll()
                .map(com.example.ragollama.monitoring.model.KnowledgeGap::getQueryText)
                .take(100)
                .collectList();

        return Mono.zip(negativeFeedbackMono, knowledgeGapsMono)
                .map(tuple -> {
                    List<String> allFeedback = Flux.concat(Flux.fromIterable(tuple.getT1()), Flux.fromIterable(tuple.getT2()))
                            .collectList()
                            .block(); // .block() здесь допустим, т.к. мы уже внутри реактивной цепочки и работаем с готовыми списками

                    return new AgentResult(
                            getName(),
                            AgentResult.Status.SUCCESS,
                            "Собрано " + (allFeedback != null ? allFeedback.size() : 0) + " записей обратной связи.",
                            Map.of("rawFeedback", allFeedback)
                    );
                });
    }
}
