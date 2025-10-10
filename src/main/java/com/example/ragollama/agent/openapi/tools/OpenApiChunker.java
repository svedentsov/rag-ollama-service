package com.example.ragollama.agent.openapi.tools;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.Operation;
import io.swagger.v3.oas.models.media.Schema;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.document.Document;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Сервис для интеллектуального разбиения объекта {@link OpenAPI} на
 * семантически значимые текстовые фрагменты (чанки).
 * <p>
 * Каждый эндпоинт и каждая схема преобразуются в отдельный {@link Document},
 * что позволяет выполнять по ним эффективный семантический поиск. Это
 * является основой для реализации "RAG на лету".
 */
@Slf4j
@Service
public class OpenApiChunker {

    /**
     * Разбивает объект спецификации на список документов-чанков.
     *
     * @param openApi Распарсенный объект {@link OpenAPI}.
     * @return Список {@link Document} для индексации.
     */
    public List<Document> split(OpenAPI openApi) {
        List<Document> chunks = new ArrayList<>();
        chunks.addAll(createPathChunks(openApi));
        chunks.addAll(createSchemaChunks(openApi));
        log.info("OpenAPI спецификация разделена на {} семантических чанков.", chunks.size());
        return chunks;
    }

    private List<Document> createPathChunks(OpenAPI openApi) {
        List<Document> pathChunks = new ArrayList<>();
        if (openApi.getPaths() == null) return pathChunks;

        openApi.getPaths().forEach((pathName, pathItem) -> {
            pathItem.readOperationsMap().forEach((method, operation) -> {
                String content = formatOperation(pathName, method.toString(), operation);
                Map<String, Object> metadata = Map.of(
                        "type", "path",
                        "pathName", pathName,
                        "method", method.toString()
                );
                pathChunks.add(new Document(content, metadata));
            });
        });
        return pathChunks;
    }

    private List<Document> createSchemaChunks(OpenAPI openApi) {
        List<Document> schemaChunks = new ArrayList<>();
        if (openApi.getComponents() == null || openApi.getComponents().getSchemas() == null) return schemaChunks;

        openApi.getComponents().getSchemas().forEach((schemaName, schema) -> {
            String content = formatSchema(schemaName, schema);
            Map<String, Object> metadata = Map.of(
                    "type", "schema",
                    "schemaName", schemaName
            );
            schemaChunks.add(new Document(content, metadata));
        });
        return schemaChunks;
    }

    private String formatOperation(String path, String method, Operation operation) {
        StringBuilder sb = new StringBuilder();
        sb.append(String.format("Endpoint: %s %s\n", method.toUpperCase(), path));
        if (operation.getSummary() != null) sb.append(String.format("Summary: %s\n", operation.getSummary()));
        if (operation.getDescription() != null)
            sb.append(String.format("Description: %s\n", operation.getDescription()));
        return sb.toString();
    }

    private String formatSchema(String name, Schema<?> schema) {
        StringBuilder sb = new StringBuilder();
        sb.append(String.format("Schema: %s\n", name));
        if (schema.getDescription() != null) sb.append(String.format("Description: %s\n", schema.getDescription()));
        if (schema.getProperties() != null) {
            String properties = schema.getProperties().entrySet().stream()
                    .map(entry -> String.format("- %s (%s)", entry.getKey(), entry.getValue().getType()))
                    .collect(Collectors.joining("\n"));
            sb.append("Properties:\n").append(properties);
        }
        return sb.toString();
    }
}
