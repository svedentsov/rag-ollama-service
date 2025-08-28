package com.example.ragollama.evaluation.domain;

import com.example.ragollama.evaluation.model.GoldenRecord;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;
import org.springframework.core.io.WritableResource;
import org.springframework.stereotype.Repository;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;

/**
 * Репозиторий для управления "золотым датасетом".
 * <p>
 * ВАЖНО: Эта реализация для демонстрации работает с локальным JSON-файлом.
 * В production-системе это должна быть таблица в базе данных.
 */
@Slf4j
@Repository
@RequiredArgsConstructor
public class GoldenRecordRepository {
    private static final String GOLDEN_DATASET_PATH = "classpath:evaluation/golden-dataset.json";
    private final ResourceLoader resourceLoader;
    private final ObjectMapper objectMapper = new ObjectMapper().enable(SerializationFeature.INDENT_OUTPUT);

    /**
     * Добавляет новую запись в "золотой датасет".
     *
     * @param newRecord Новая запись для добавления.
     * @throws IOException если произошла ошибка при чтении или записи файла.
     */
    public synchronized void save(GoldenRecord newRecord) throws IOException {
        Resource resource = resourceLoader.getResource(GOLDEN_DATASET_PATH);
        List<GoldenRecord> records;

        try (InputStream inputStream = resource.getInputStream()) {
            records = objectMapper.readValue(inputStream, new TypeReference<>() {
            });
        } catch (IOException e) {
            log.warn("Не удалось прочитать существующий golden-dataset.json, будет создан новый.", e);
            records = new ArrayList<>();
        }

        records.add(newRecord);

        if (resource instanceof WritableResource writableResource) {
            try (OutputStream outputStream = writableResource.getOutputStream()) {
                objectMapper.writeValue(outputStream, records);
                log.info("Новая запись GoldenRecord с ID '{}' успешно добавлена в датасет.", newRecord.queryId());
            }
        } else {
            log.error("Не удается записать в ресурс: {}. Убедитесь, что приложение запущено не из JAR-архива.", resource.getURI());
            throw new IOException("Ресурс не является записываемым.");
        }
    }
}
