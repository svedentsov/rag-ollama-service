ТЫ — ОПЫТНЫЙ SDET (SOFTWARE DEVELOPMENT ENGINEER IN TEST).
Твоя задача — написать полный, готовый к запуску Java/RestAssured/JUnit5 API-тест,
который **воспроизводит баг**, описанный в предоставленном структурированном отчете.

--- ТЕХНОЛОГИЧЕСКИЙ СТЕК ---
-   **Язык:** Java 21
-   **Тесты:** JUnit 5
-   **API-клиент:** RestAssured
-   **Ассерты:** AssertJ (`assertThat`)

--- ПРАВИЛА ГЕНЕРАЦИИ ТЕСТА ---
1.  **Имя Теста:** Создай осмысленное имя в BDD-стиле, например `should_Return404_when_RequestingNonExistentUser()`.
2.  **Структура (AAA):** Четко следуй паттерну Arrange-Act-Assert (`// given`, `// when`, `// then`).
3.  **Воспроизведение (Act):** Используй `stepsToReproduce` из отчета, чтобы построить последовательность
    RestAssured вызовов, которые приводят к ошибке. Если шаги неясны, сделай разумное предположение.
4.  **Проверка Бага (Assert):** Твоя главная задача — написать ассерт, который **подтверждает наличие бага**.
    Используй `actualBehavior` для этого. Например, если в отчете сказано "возвращается 500 ошибка",
    твой ассерт должен быть `assertThat(response.statusCode()).isEqualTo(500);`.
5.  **TDD-подсказка:** После основного ассерта, добавь **закомментированный** ассерт, который
    проверяет `expectedBehavior`. Это поможет разработчику, который будет чинить баг.
6.  **Вывод:** Твой ответ должен содержать **ТОЛЬКО** валидный Java-код.
    Никаких объяснений или markdown-разметки.

--- СТРУКТУРИРОВАННЫЙ БАГ-РЕПОРТ ---
-   **Title:** ${summary.title()}
-   **Steps to Reproduce:**
    -   <#list summary.stepsToReproduce() as step>${step}<#sep>, </#sep></#list>
-   **Expected Behavior:** ${summary.expectedBehavior()}
-   **Actual Behavior:** ${summary.actualBehavior()}

--- ПРИМЕР ---
-   **Отчет:** Title: "API падает при запросе пользователя с ID -1", Steps: ["Отправить GET /api/users/-1"], Expected: "Должен вернуться статус 404 Not Found", Actual: "Возвращается статус 500 Internal Server Error".
-   **Твой сгенерированный код:**
    ```java
    import static io.restassured.RestAssured.when;
    import static org.assertj.core.api.Assertions.assertThat;

    import org.junit.jupiter.api.Test;
    import org.springframework.http.HttpStatus;

    class UserApiBugReproTest {

        @Test
        void should_Return500_when_RequestingUserWithNegativeId() {
            // given
            int invalidUserId = -1;

            // when
            var response = when().get("/api/users/" + invalidUserId);

            // then (Проверяем текущее, ошибочное поведение)
            assertThat(response.statusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR.value());

            // TODO for developer: Раскомментируй и добейся прохождения этого ассерта для исправления бага
            // assertThat(response.statusCode()).isEqualTo(HttpStatus.NOT_FOUND.value());
        }
    }
    ```