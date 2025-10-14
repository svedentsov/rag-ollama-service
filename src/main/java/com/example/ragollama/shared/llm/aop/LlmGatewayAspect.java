package com.example.ragollama.shared.llm.aop;

import com.example.ragollama.agent.finops.domain.LlmUsageTracker;
import com.example.ragollama.agent.finops.domain.QuotaService;
import com.example.ragollama.shared.exception.QuotaExceededException;
import com.example.ragollama.shared.llm.model.LlmResponse;
import com.example.ragollama.shared.tokenization.TokenizationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.ollama.api.OllamaOptions;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

/**
 * Аспект для применения сквозных задач (проверка квот, логирование)
 * к вызовам LLM-шлюза в реактивном стиле.
 */
@Aspect
@Component
@Slf4j
@RequiredArgsConstructor
public class LlmGatewayAspect {
    private final QuotaService quotaService;
    private final LlmUsageTracker usageTracker;
    private final TokenizationService tokenizationService;

    /**
     * Advice, выполняющий проверку квот перед вызовом LLM.
     * <p>
     * Эта версия корректно обрабатывает как {@link Mono}, так и {@link Flux},
     * возвращаемые целевым методом, используя {@code flatMap} и {@code flatMapMany}
     * соответственно. Для определения возвращаемого типа используется {@link MethodSignature}.
     *
     * @param joinPoint Точка соединения, представляющая вызов метода.
     * @return Результат выполнения метода (Mono или Flux).
     * @throws Throwable если выполнение завершилось исключением.
     */
    @Around("@annotation(com.example.ragollama.shared.llm.aop.LlmQuotaCheck)")
    public Object checkQuota(ProceedingJoinPoint joinPoint) throws Throwable {
        String username = "anonymous_user";
        Prompt prompt = (Prompt) joinPoint.getArgs()[0];
        int promptTokens = tokenizationService.countTokens(prompt.getContents());
        Mono<Boolean> quotaExceededMono = quotaService.isQuotaExceeded(username, promptTokens);
        MethodSignature signature = (MethodSignature) joinPoint.getSignature();
        Class<?> returnType = signature.getReturnType();
        boolean returnsFlux = Flux.class.isAssignableFrom(returnType);
        if (returnsFlux) {
            return quotaExceededMono.flatMapMany(isExceeded -> {
                if (isExceeded) {
                    return Flux.error(new QuotaExceededException("Месячный лимит токенов исчерпан."));
                }
                try {
                    return (Flux<?>) joinPoint.proceed();
                } catch (Throwable e) {
                    return Flux.error(e);
                }
            });
        } else {
            return quotaExceededMono.flatMap(isExceeded -> {
                if (isExceeded) {
                    return Mono.error(new QuotaExceededException("Месячный лимит токенов исчерпан."));
                }
                try {
                    return (Mono<?>) joinPoint.proceed();
                } catch (Throwable e) {
                    return Mono.error(e);
                }
            });
        }
    }

    /**
     * Advice, выполняющий логирование использования токенов после успешного вызова LLM.
     *
     * @param joinPoint Точка соединения, представляющая вызов метода.
     * @return Результат выполнения метода (Mono или Flux) с добавленной логикой логирования.
     * @throws Throwable если выполнение завершилось исключением.
     */
    @Around("@annotation(com.example.ragollama.shared.llm.aop.LlmUsageTracking)")
    public Object trackUsage(ProceedingJoinPoint joinPoint) throws Throwable {
        OllamaOptions options = (OllamaOptions) joinPoint.getArgs()[1];
        String modelName = options.getModel();
        Object returnValue = joinPoint.proceed();
        if (returnValue instanceof Mono<?> mono) {
            return mono.doOnSuccess(response -> {
                if (response instanceof ChatResponse chatResponse) {
                    track(modelName, new LlmResponse(null, chatResponse.getMetadata().getUsage(), null));
                }
            });
        }
        if (returnValue instanceof Flux<?> flux) {
            return flux.doOnNext(response -> {
                if (response instanceof ChatResponse chatResponse && chatResponse.getMetadata().getUsage() != null) {
                    track(modelName, new LlmResponse(null, chatResponse.getMetadata().getUsage(), null));
                }
            });
        }
        return returnValue;
    }

    private void track(String modelName, LlmResponse response) {
        if (response != null && response.usage() != null) {
            usageTracker.trackUsage(modelName, response).subscribe(
                    null, // onNext не нужен
                    error -> log.error("Аспект: не удалось асинхронно записать использование LLM.", error)
            );
        }
    }
}
