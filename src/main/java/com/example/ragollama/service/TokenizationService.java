package com.example.ragollama.service;

import com.example.ragollama.config.properties.AppProperties;
import com.knuddels.jtokkit.Encodings;
import com.knuddels.jtokkit.api.Encoding;
import com.knuddels.jtokkit.api.EncodingRegistry;
import com.knuddels.jtokkit.api.EncodingType;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import java.util.Optional;

/**
 * Сервис для подсчета токенов в тексте.
 * <p>
 * Инкапсулирует работу с библиотекой jtokkit и предоставляет простой
 * интерфейс для подсчета токенов. Выделен в отдельный сервис для
 * следования принципу единственной ответственности и возможности
 * переиспользования в других частях приложения.
 */
@Service
@Slf4j
public class TokenizationService {

    private final String encodingModel;
    private Encoding encoding;

    /**
     * Конструктор, использующий централизованный бин конфигурации.
     *
     * @param appProperties Объект с настройками приложения.
     */
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
     * <p>
     * Результат кэшируется, так как токенизация одного и того же текста
     * всегда дает одинаковый результат. Это может быть полезно, если
     * одни и те же чанки документов часто попадают в контекст.
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
}
