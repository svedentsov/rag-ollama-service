package com.example.ragollama.shared.llm.aop;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Маркерная аннотация для методов, выполняющих вызовы к LLM, которые
 * должны проходить проверку на соответствие квотам.
 * <p>
 * Применение этой аннотации активирует аспект {@link LlmGatewayAspect},
 * который выполнит проверку квоты перед фактическим вызовом метода.
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface LlmQuotaCheck {
}
