package com.example.ragollama.qaagent.tools;

import org.springframework.stereotype.Service;

/**
 * Утилитарный сервис для парсинга OpenAPI/Swagger спецификаций.
 * <p>
 * В реальном приложении здесь будет использоваться библиотека типа Swagger Parser.
 * Для демонстрации, мы реализуем простой mock, который извлекает текстовое описание эндпоинта.
 */
@Service
public class OpenApiParser {

    /**
     * Извлекает детали указанного эндпоинта из спецификации.
     *
     * @param openApiContent Содержимое OpenAPI файла в виде строки.
     * @param endpointName   Имя эндпоинта для анализа (например, "GET /api/v1/users/{id}").
     * @return Человекочитаемое описание эндпоинта для передачи в LLM.
     */
    public String extractEndpointDetails(String openApiContent, String endpointName) {
        // NOTE: Это mock-реализация. В production здесь будет полноценный парсинг.
        // Здесь мы просто вернем заглушку для демонстрации.
        if ("POST /api/v1/documents".equalsIgnoreCase(endpointName)) {
            return """
                    Endpoint: POST /api/v1/documents
                    Description: Принимает документ и ставит его в очередь на фоновую обработку.
                    Request Body: DocumentIngestionRequest
                    - sourceName (string, required): Имя источника документа.
                    - text (string, required): Полный текст документа.
                    Responses:
                    - 202 Accepted: JobSubmissionResponse { jobId: UUID }
                    - 400 Bad Request: Error details.
                    """;
        }
        return "Детали для эндпоинта '" + endpointName + "' не найдены.";
    }
}
