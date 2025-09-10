package com.example.ragollama.agent.accessibility.api;

import com.example.ragollama.agent.AgentOrchestratorService;
import com.example.ragollama.agent.accessibility.api.dto.AccessibilityAuditRequest;
import com.example.ragollama.agent.accessibility.api.dto.AccessibilityAuditResponse;
import com.example.ragollama.agent.accessibility.mappers.AccessibilityMapper;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.concurrent.CompletableFuture;

/**
 * Контроллер для AI-агента, выполняющего аудит доступности (a11y).
 * <p>
 * Этот контроллер является эталонной реализацией Web-слоя в Clean Architecture.
 * Его обязанности строго ограничены:
 * <ul>
 *     <li>Прием и валидация входящих DTO (Data Transfer Objects).</li>
 *     <li>Делегирование всей бизнес-логики сервисному слою ({@link AgentOrchestratorService}).</li>
 *     <li>Преобразование внутреннего результата в публичный DTO ответа с помощью маппера.</li>
 * </ul>
 * <p>
 * Благодаря использованию {@link CompletableFuture}, контроллер работает в полностью
 * неблокирующем режиме, немедленно освобождая поток запроса и асинхронно
 * ожидая результат выполнения задачи.
 */
@RestController
@RequestMapping("/api/v1/agents/accessibility")
@RequiredArgsConstructor
@Tag(name = "Accessibility Agent (a11y)", description = "API для аудита доступности UI")
public class AccessibilityAgentController {

    private final AgentOrchestratorService orchestratorService;
    private final AccessibilityMapper accessibilityMapper;

    /**
     * Запускает асинхронный конвейер для анализа HTML-кода на предмет нарушений доступности.
     *
     * @param request DTO с HTML-кодом для анализа. Должен быть валидным.
     * @return {@link CompletableFuture}, который по завершении будет содержать
     * результат работы конвейера, преобразованный в DTO ответа. Spring MVC
     * автоматически обработает асинхронный ответ.
     */
    @PostMapping("/audit")
    @Operation(
            summary = "Провести аудит доступности (a11y) для HTML-кода",
            description = "Принимает HTML-код страницы и асинхронно запускает конвейер для его анализа. " +
                    "Возвращает отчет с резюме, рекомендациями и списком технических нарушений."
    )
    @ApiResponse(
            responseCode = "200",
            description = "Аудит успешно завершен",
            content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE, schema = @Schema(implementation = AccessibilityAuditResponse.class))
    )
    @ApiResponse(responseCode = "400", description = "Некорректный запрос (например, пустой HTML)")
    public CompletableFuture<AccessibilityAuditResponse> auditAccessibility(@Valid @RequestBody AccessibilityAuditRequest request) {
        return orchestratorService.invokePipeline("accessibility-audit-pipeline", request.toAgentContext())
                .thenApply(accessibilityMapper::toResponseDto);
    }
}
