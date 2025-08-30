package com.example.ragollama.agent.events.security;

import jakarta.validation.constraints.NotNull;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.util.Formatter;

/**
 * Сервис для проверки подписей входящих веб-хуков от GitHub.
 * <p>
 * Реализует криптографическую проверку HMAC-SHA256, что является
 * стандартным и безопасным способом убедиться, что веб-хук был отправлен
 * именно из GitHub, а не подделан злоумышленником.
 */
@Service
@RequiredArgsConstructor
public class WebhookSecurityService {

    @Value("${app.integrations.github.webhook-secret}")
    private String secret;

    private static final String HMAC_SHA256 = "HmacSHA256";

    /**
     * Проверяет подпись GitHub веб-хука.
     *
     * @param payload   Тело запроса в виде сырой строки.
     * @param signature Заголовок 'X-Hub-Signature-256' из запроса.
     * @return `true`, если подпись верна, иначе `false`.
     */
    public boolean isValidGitHubSignature(String payload, String signature) {
        if (!StringUtils.hasText(signature) || !signature.startsWith("sha256=")) {
            return false;
        }
        String expectedSignature = "sha256=" + calculateHmac(payload);
        return signature.equals(expectedSignature);
    }

    /**
     * Вычисляет HMAC-SHA256 для строки данных.
     *
     * @param data Данные для хеширования (тело запроса).
     * @return Шестнадцатеричное представление хеша.
     */
    private String calculateHmac(@NotNull String data) {
        try {
            SecretKeySpec secretKeySpec = new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), HMAC_SHA256);
            Mac mac = Mac.getInstance(HMAC_SHA256);
            mac.init(secretKeySpec);
            byte[] hmac = mac.doFinal(data.getBytes(StandardCharsets.UTF_8));
            return toHexString(hmac);
        } catch (NoSuchAlgorithmException | InvalidKeyException e) {
            throw new RuntimeException("Не удалось вычислить HMAC-подпись", e);
        }
    }

    /**
     * Преобразует массив байт в строку hex.
     *
     * @param bytes Массив байт.
     * @return Строка в шестнадцатеричном формате.
     */
    private String toHexString(byte[] bytes) {
        try (Formatter formatter = new Formatter()) {
            for (byte b : bytes) {
                formatter.format("%02x", b);
            }
            return formatter.toString();
        }
    }
}
