package com.example.ragollama.qaagent.model;

/**
 * Представляет результат одного тест-кейса, извлеченный из JUnit XML отчета.
 * <p>
 * В этой версии добавлено поле `failureDetails` для хранения полного
 * текста ошибки и стек-трейса, что является критически важной информацией
 * для агента анализа первопричин (RCA).
 *
 * @param className      Имя класса теста.
 * @param testName       Имя тестового метода.
 * @param status         Статус выполнения теста.
 * @param failureDetails Полный текст ошибки и стек-трейс, если тест упал.
 */
public record TestResult(
        String className,
        String testName,
        Status status,
        String failureDetails
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
