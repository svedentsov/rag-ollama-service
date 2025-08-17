package com.example.ragollama.aop;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Inherited;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Маркерная аннотация для методов, выполняющих операции с базой данных,
 * которые должны быть защищены механизмами отказоустойчивости Resilience4j.
 * <p>
 * Применение этой аннотации к методу активирует аспект {@link DatabaseResilienceAspect},
 * который оборачивает вызов в логику Retry и Circuit Breaker,
 * сконфигурированную в {@code application.yml} под инстансом "database".
 */
@Target({ElementType.METHOD, ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
@Inherited
@Documented
public @interface ResilientDatabaseOperation {
}
