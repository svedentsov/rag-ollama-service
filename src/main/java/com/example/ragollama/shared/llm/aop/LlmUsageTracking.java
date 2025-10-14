package com.example.ragollama.shared.llm.aop;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Маркерная аннотация для методов, выполняющих вызовы к LLM,
 * использование которых должно быть залогировано для FinOps.
 * <p>
 * Применение этой аннотации активирует аспект {@link LlmGatewayAspect},
 * который асинхронно запишет информацию об использованных токенах
 * после успешного выполнения метода.
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface LlmUsageTracking {
}
