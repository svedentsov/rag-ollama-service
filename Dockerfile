# --- Этап 1: Сборка бэкенда ---
# Используем официальный образ Gradle с нужной версией JDK.
FROM gradle:8.8-jdk21 AS builder

WORKDIR /app

# Копируем файлы Gradle для кэширования зависимостей
COPY build.gradle settings.gradle ./
COPY gradlew .
COPY gradle ./gradle
RUN ./gradlew dependencies --no-daemon

# Копируем исходный код бэкенда
COPY src ./src

# Собираем приложение в исполняемый JAR-файл.
RUN ./gradlew build -x test --no-daemon


# --- Этап 2: Финальный образ ---
# Используем минималистичный образ с JRE.
FROM eclipse-temurin:21-jre-jammy

WORKDIR /app

# Создаем системного пользователя и группу с ограниченными правами.
RUN groupadd --system --gid 1000 appgroup && useradd --system --uid 1000 --gid appgroup appuser

# Копируем собранный JAR-файл бэкенда
COPY --from=builder /app/build/libs/app.jar app.jar

# Устанавливаем владельца для файлов приложения.
RUN chown appuser:appgroup app.jar

# Переключаемся на пользователя с ограниченными правами.
USER appuser

EXPOSE 8080

ENTRYPOINT ["java", "-jar", "app.jar"]