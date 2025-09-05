package com.example.ragollama.agent.copilot.api;

import com.example.ragollama.agent.copilot.QaCopilotService;
import com.example.ragollama.agent.copilot.api.dto.CopilotRequest;
import com.example.ragollama.agent.copilot.api.dto.CopilotResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;

/**
 * Контроллер для stateful-взаимодействия с QA Copilot.
 * <p>
 * Предоставляет чат-интерфейс для выполнения сложных, многошаговых
 * задач с помощью динамических конвейеров агентов.
 */
@RestController
@RequestMapping("/api/v1/copilot")
@RequiredArgsConstructor
@Tag(name = "QA Copilot", description = "Чат-интерфейс для взаимодействия с AI-агентами")
public class QaCopilotController {

    private final QaCopilotService qaCopilotService;

    /**
     * Отправляет сообщение в диалоговую сессию с QA Copilot.
     *
     * @param request DTO с сообщением пользователя и опциональным ID сессии.
     * @return {@link Mono} с ответом от ассистента.
     */
    @PostMapping("/chat")
    @Operation(summary = "Отправить сообщение в чат с QA Copilot",
            description = "Продолжает существующий диалог или начинает новый. " +
                    "Копайлот сохраняет контекст между сообщениями.")
    public Mono<CopilotResponse> chatWithCopilot(@Valid @RequestBody CopilotRequest request) {
        return qaCopilotService.processUserMessage(request);
    }
}
