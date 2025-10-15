package com.example.ragollama.orchestration.validation;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Кастомная аннотация валидации, которая проверяет, что в запросе присутствует
 * либо текстовый запрос (query), либо контекст (например, содержимое файла).
 * <p>
 * Применяется на уровне класса к DTO {@link com.example.ragollama.orchestration.dto.UniversalRequest}.
 */
@Target({ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
@Constraint(validatedBy = UniversalRequestValidator.class)
public @interface ValidRequestPayload {
    String message() default "Запрос должен содержать либо непустой 'query', либо 'context'.";
    Class<?>[] groups() default {};
    Class<? extends Payload>[] payload() default {};
}
