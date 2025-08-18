package com.example.ragollama.shared.tokenization;

import com.example.ragollama.shared.config.properties.AppProperties;
import com.knuddels.jtokkit.Encodings;
import com.knuddels.jtokkit.api.Encoding;
import com.knuddels.jtokkit.api.EncodingRegistry;
import com.knuddels.jtokkit.api.EncodingType;
import com.knuddels.jtokkit.api.IntArrayList;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import java.util.Optional;

/**
 * Сервис для подсчета и манипуляции токенами в тексте.
 * Инкапсулирует работу с высокопроизводительной библиотекой jtokkit,
 * корректно обрабатывая ее специфичные типы данных, такие как {@link IntArrayList}.
 */
@Slf4j
@Service
public class TokenizationService {

    private final String encodingModel;
    private Encoding encoding;

    public TokenizationService(AppProperties appProperties) {
        this.encodingModel = appProperties.tokenization().encodingModel();
    }

    @PostConstruct
    public void init() {
        EncodingRegistry registry = Encodings.newDefaultEncodingRegistry();
        try {
            Optional<Encoding> opt = registry.getEncoding(encodingModel);
            if (opt.isPresent()) {
                this.encoding = opt.get();
                log.info("TokenizationService успешно инициализирован с моделью кодирования: {}", encodingModel);
            } else {
                log.warn("Кодировка '{}' не найдена в реестре. Попытка использовать fallback: {}", encodingModel, EncodingType.O200K_BASE.getName());
                this.encoding = registry.getEncoding(EncodingType.O200K_BASE.getName())
                        .orElseThrow(() -> new IllegalStateException("Fallback encoding not available: " + EncodingType.O200K_BASE.getName()));
                log.warn("Используется fallback-модель кодирования: {}", EncodingType.O200K_BASE.getName());
            }
        } catch (Exception e) {
            log.error("Ошибка при инициализации TokenizationService с моделью '{}'.", encodingModel, e);
            EncodingRegistry fallbackRegistry = Encodings.newDefaultEncodingRegistry();
            this.encoding = fallbackRegistry.getEncoding(EncodingType.O200K_BASE.getName())
                    .orElseThrow(() -> new IllegalStateException("Fallback encoding not available: " + EncodingType.O200K_BASE.getName(), e));
            log.warn("После ошибки используется fallback-модель кодирования: {}", EncodingType.O200K_BASE.getName());
        }
    }

    /**
     * Подсчитывает количество токенов в заданной строке.
     *
     * @param text Текст для токенизации.
     * @return Количество токенов.
     */
    @Cacheable(value = "token_counts", key = "#text")
    public int countTokens(String text) {
        if (text == null || text.isEmpty()) {
            return 0;
        }
        if (encoding == null) {
            throw new IllegalStateException("TokenizationService не инициализирован: encoding == null");
        }
        return encoding.countTokens(text);
    }

    /**
     * Обрезает текст до заданного лимита токенов.
     * <p>
     * Этот метод корректно работает с токенами, используя нативный для
     * jtokkit тип {@link IntArrayList} для максимальной производительности.
     * Он кодирует текст, берет необходимое количество токенов и декодирует
     * их обратно в строку.
     *
     * @param text      Исходный текст.
     * @param maxTokens Максимальное количество токенов в результирующей строке.
     * @return Обрезанная строка.
     */
    public String truncate(String text, int maxTokens) {
        if (text == null || text.isEmpty() || maxTokens <= 0) {
            return "";
        }
        if (encoding == null) {
            throw new IllegalStateException("TokenizationService не инициализирован: encoding == null");
        }

        // Используем IntArrayList
        IntArrayList tokens = encoding.encode(text);
        if (tokens.size() <= maxTokens) {
            return text;
        }

        // Создаем новый IntArrayList из подсписка
        IntArrayList truncatedTokens = new IntArrayList(maxTokens);
        for (int i = 0; i < maxTokens; i++) {
            truncatedTokens.add(tokens.get(i));
        }

        return encoding.decode(truncatedTokens);
    }
}
