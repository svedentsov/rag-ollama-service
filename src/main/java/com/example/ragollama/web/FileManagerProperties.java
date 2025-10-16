package com.example.ragollama.web;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

import java.util.List;

/**
 * Типобезопасная конфигурация для файлового менеджера.
 * Этот класс является "чистым" DTO. Его регистрация как Spring-бина
 * управляется централизованно через @EnableConfigurationProperties.
 */
@ConfigurationProperties(prefix = "app.file-storage")
@Validated
@Getter
@Setter
public class FileManagerProperties {

    @NotBlank
    private String uploadDir;

    @Min(1)
    @Max(100)
    private int maxFileSizeMb = 10;

    @NotEmpty
    private List<String> allowedMimeTypes;
}
