package com.example.ragollama;

import com.example.ragollama.agent.config.*;
import com.example.ragollama.crawler.confluence.ConfluenceProperties;
import com.example.ragollama.evaluation.EvaluationProperties;
import com.example.ragollama.ingestion.IngestionProperties;
import com.example.ragollama.optimization.IndexOptimizerProperties;
import com.example.ragollama.rag.domain.reranking.RerankingProperties;
import com.example.ragollama.rag.retrieval.RetrievalProperties;
import com.example.ragollama.shared.config.properties.AppProperties;
import com.example.ragollama.shared.llm.LlmRouterService;
import com.example.ragollama.shared.processing.PiiRedactionService;
import com.example.ragollama.web.FileManagerProperties;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * Главный класс приложения Spring Boot.
 * <p>
 * Активирует все основные возможности фреймворка и регистрирует все
 * классы, управляемые через {@code @ConfigurationProperties}.
 */
@SpringBootApplication
@EnableCaching
@EnableAsync
@EnableScheduling
@EnableConfigurationProperties({
        AppProperties.class,
        RetrievalProperties.class,
        RerankingProperties.class,
        IngestionProperties.class,
        EvaluationProperties.class,
        ConfluenceProperties.class,
        JiraProperties.class,
        GitProperties.class,
        PiiRedactionService.PiiRedactionProperties.class,
        LlmRouterService.LlmProperties.class,
        IndexOptimizerProperties.class,
        CiProperties.class,
        Neo4jProperties.class,
        FlakinessProperties.class,
        CostProperties.class,
        FederationProperties.class,
        KnowledgeDomainProperties.class,
        QuotaProperties.class,
        FileManagerProperties.class})
public class RagOllamaApplication {
    /**
     * Точка входа в приложение.
     *
     * @param args Аргументы командной строки.
     */
    public static void main(String[] args) {
        SpringApplication.run(RagOllamaApplication.class, args);
    }
}
