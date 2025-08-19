package com.example.ragollama;

import com.example.ragollama.rag.retrieval.RetrievalProperties;
import com.example.ragollama.shared.config.properties.AppProperties;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableCaching
@EnableAsync
@EnableScheduling
@EnableConfigurationProperties({AppProperties.class, RetrievalProperties.class})
public class RagOllamaApplication {

    public static void main(String[] args) {
        SpringApplication.run(RagOllamaApplication.class, args);
    }
}
