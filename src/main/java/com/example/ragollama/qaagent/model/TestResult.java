package com.example.ragollama.qaagent.model;

/**
 * Представляет результат одного тест-кейса, извлеченный из JUnit XML отчета.
 * <p>
 * Этот record является неизменяемым DTO, который используется для передачи
 * распарсенных данных от {@link com.example.ragollama.qaagent.tools.JUnitXmlParser}
 * к агентам-обработчикам.
 *
 * @param className      Имя класса теста.
 * @param testName       Имя тестового метода.
 * @param status         Статус выполнения теста (PASSED, FAILED, SKIPPED).
 * @param failureDetails Полный текст ошибки и стек-трейс, если тест упал.
 *                       Может быть {@code null} для успешно пройденных тестов.
 * @param durationMs     Продолжительность выполнения теста в миллисекундах.
 */
public record TestResult(
        String className,
        String testName,
        Status status,
        String failureDetails,
        long durationMs
) {
    /**
     * Перечисление возможных статусов теста.
     */
    public enum Status {
        PASSED, FAILED, SKIPPED
    }

    /**
     * Возвращает полное, уникальное имя теста, комбинируя имя класса и метода.
     *
     * @return Строка вида "com.example.UserServiceTest.shouldCreateUser".
     */
    public String getFullName() {
        return className + "." + testName;
    }
}
