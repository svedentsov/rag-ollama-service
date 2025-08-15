package com.example.ragollama.config;

import org.slf4j.MDC;
import org.springframework.core.task.TaskDecorator;

import java.util.Map;

/**
 * TaskDecorator для Spring, который обеспечивает передачу контекста MDC (Mapped Diagnostic Context)
 * из родительского потока в дочерний поток, выполняющий асинхронную задачу.
 * Это необходимо для сквозной трассировки запросов по requestId в логах.
 */
public class MdcTaskDecorator implements TaskDecorator {

    @Override
    public Runnable decorate(Runnable runnable) {
        // Захватываем контекст из родительского потока (например, потока Tomcat)
        Map<String, String> contextMap = MDC.getCopyOfContextMap();

        // Возвращаем новую Runnable, которая сначала установит контекст,
        // а затем выполнит исходную задачу.
        return () -> {
            try {
                // Устанавливаем захваченный контекст в дочернем потоке (из пула)
                if (contextMap != null) {
                    MDC.setContextMap(contextMap);
                }
                // Выполняем оригинальную задачу
                runnable.run();
            } finally {
                // Обязательно очищаем MDC после выполнения, чтобы избежать
                // утечки контекста в другие задачи, которые могут выполняться в этом же потоке.
                MDC.clear();
            }
        };
    }
}
