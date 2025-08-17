package com.example.ragollama.rag.advisor;

import com.example.ragollama.rag.RagAdvisor;
import com.example.ragollama.rag.RagContext;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

import java.util.Optional;

/**
 * Советник, который анализирует запрос пользователя на предмет упоминания
 * конкретных источников и, если таковые найдены в извлеченных документах,
 * добавляет в промпт специальную инструкцию для LLM.
 * <p>
 * Эта техника позволяет значительно повысить точность ответов на вопросы,
 * сфокусированные на конкретном документе, явно направляя "внимание" модели.
 */
@Slf4j
@Component
@Order(20) // Выполняется после базовых советников
public class MetadataFilteringAdvisor implements RagAdvisor {

    /**
     * Анализирует контекст и добавляет инструкцию по приоритезации источника, если это необходимо.
     *
     * @param context Текущий контекст RAG-запроса, содержащий запрос пользователя и найденные документы.
     * @return {@link Mono} с обновленным контекстом, готовый для следующего шага в конвейере.
     */
    @Override
    public Mono<RagContext> advise(RagContext context) {
        final String query = context.getOriginalQuery().toLowerCase();

        // Ищем в извлеченных документах тот, чье имя источника (source) упоминается в запросе.
        Optional<String> mentionedSource = context.getDocuments().stream()
                .map(doc -> (String) doc.getMetadata().get("source"))
                .filter(sourceName -> sourceName != null && query.contains(sourceName.toLowerCase()))
                .findFirst();

        if (mentionedSource.isPresent()) {
            String sourceName = mentionedSource.get();
            String instruction = String.format(
                    "При ответе на вопрос удели особое внимание информации из источника '%s'.", sourceName
            );
            context.getPromptModel().put("priority_source_instruction", instruction);
            log.info("MetadataFilteringAdvisor: найден приоритетный источник '{}'. Добавлена инструкция в промпт.", sourceName);
        } else {
            // Важно добавлять пустую строку, чтобы шаблон не сломался, если переменная не найдена
            context.getPromptModel().put("priority_source_instruction", "");
        }

        return Mono.just(context);
    }
}
