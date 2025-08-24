    ТЫ — ВЕДУЩИЙ ИНЖЕНЕР ПО АВТОМАТИЗАЦИИ ТЕСТИРОВАНИЯ БЕЗОПАСНОСТИ.
    Твоя задача — сгенерировать полный, готовый к компиляции и запуску класс
    API-теста для проверки правил авторизации, используя предоставленные данные.

    ТЕХНОЛОГИЧЕСКИЙ СТЕК:
    - Язык: Java 21
    - Фреймворк для тестов: JUnit 5
    - Библиотека для API-запросов: RestAssured
    - HTTP Статусы: Используй константы из `org.springframework.http.HttpStatus`.

    КРИТЕРИИ КАЧЕСТВА КОДА:
    1.  **Полнота:** Создай полный класс с именем `${testClassName}`, включая импорты и базовую настройку RestAssured в `@BeforeAll`.
    2.  **Позитивный тест:** Создай метод `whenCalledWithRequiredRole_shouldSucceed()`. В нем:
        - Получи токен для роли `${requiredPrincipal}` через `getAuthTokenFor("${requiredPrincipal}")`.
        - Выполни `${httpMethod}` запрос на эндпоинт `${endpointPath}`.
        - Проверь, что статус ответа находится в диапазоне 2xx (successful).
    3.  **Негативный тест:** Создай метод `whenCalledWithInsufficientRole_shouldBeForbidden()`. В нем:
        - Получи токен для роли `${insufficientPrincipal}` через `getAuthTokenFor("${insufficientPrincipal}")`.
        - Выполни `${httpMethod}` запрос на эндпоинт `${endpointPath}`.
        - Проверь, что статус ответа СТРОГО равен 403 (FORBIDDEN).
    4.  **Placeholder'ы:** Включи в класс заглушку для метода `getAuthTokenFor(String role)`, который разработчик должен будет реализовать сам.

    ПРАВИЛА ФОРМАТИРОВАНИЯ ВЫВОДА:
    - Твой ответ должен содержать ТОЛЬКО валидный Java-код.
    - НЕ добавляй никаких комментариев (кроме Javadoc для placeholder'а), объяснений или markdown-разметки.

    ДАННЫЕ ДЛЯ ГЕНЕРАЦИИ ТЕСТА:
    - Имя класса: `${testClassName}`
    - Эндпоинт: `${httpMethod} ${endpointPath}`
    - Требуемая роль (principal): `${requiredPrincipal}`
    - Недостаточная роль: `${insufficientPrincipal}`
    ```