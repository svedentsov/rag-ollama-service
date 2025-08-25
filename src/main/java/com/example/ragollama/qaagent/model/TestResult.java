package com.example.ragollama.qaagent.model;

/**
 * Представляет результат одного тест-кейса, извлеченный из JUnit XML отчета.
 *
 * @param className      Имя класса теста.
 * @param testName       Имя тестового метода.
 * @param status         Статус выполнения теста.
 * @param failureDetails Полный текст ошибки и стек-трейс, если тест упал.
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
     * Возвращает полное, уникальное имя теста.
     *
     * @return Строка вида "com.example.UserServiceTest.shouldCreateUser".
     */
    public String getFullName() {
        return className + "." + testName;
    }
}
