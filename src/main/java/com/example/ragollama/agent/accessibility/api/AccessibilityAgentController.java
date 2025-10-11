package com.example.ragollama.agent.accessibility.api;

import com.example.ragollama.agent.AgentOrchestratorService;
import com.example.ragollama.agent.AgentResult;
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
import reactor.core.publisher.Mono;

import java.util.List;

/**
 * Контроллер для AI-агента, выполняющего аудит доступности (a11y).
 * <p>
 * Эталонная реализация Web-слоя в Clean Architecture.
 * Его обязанности строго ограничены:
 * <ul>
 *     <li>Прием и валидация входящих DTO (Data Transfer Objects).</li>
 *     <li>Делегирование всей бизнес-логики сервисному слою ({@link AgentOrchestratorService}).</li>
 *     <li>Преобразование внутреннего результата в публичный DTO ответа с помощью маппера.</li>
 * </ul>
 * <p>
 * Благодаря использованию {@link reactor.core.publisher.Mono}, контроллер работает в полностью
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
     * Запускает асинхронный аудит HTML-кода на предмет нарушений доступности.
     * <p>
     * Вызывает именованный конвейер {@code accessibility-audit-pipeline}, который инкапсулирует
     * всю логику выполнения. Это делает контроллер декларативным и независимым от деталей реализации.
     *
     * @param request DTO с HTML-кодом для анализа. Должен быть валидным.
     * @return {@link Mono}, который по завершении будет содержать результат работы конвейера,
     * преобразованный в DTO ответа. Spring WebFlux автоматически обработает асинхронный ответ.
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
    public Mono<AccessibilityAuditResponse> auditAccessibility(@Valid @RequestBody AccessibilityAuditRequest request) {
        return orchestratorService.invoke("accessibility-audit-pipeline", request.toAgentContext())
                .map(this::extractFinalResult)
                .map(accessibilityMapper::toResponseDto);
    }

    /**
     * Извлекает финальный результат из списка, возвращаемого оркестратором.
     * <p>
     * В конвейере из одного шага мы ожидаем ровно один результат. Этот метод
     * обеспечивает дополнительную проверку на соответствие этому контракту.
     *
     * @param results Список результатов от оркестратора.
     * @return Единственный {@link AgentResult} из списка.
     * @throws IllegalStateException если список результатов пуст или содержит более одного элемента.
     */
    private AgentResult extractFinalResult(List<AgentResult> results) {
        if (results == null || results.size() != 1) {
            throw new IllegalStateException("Внутренняя ошибка: конвейер аудита доступности вернул некорректное количество результатов.");
        }
        return results.get(0);
    }
}
