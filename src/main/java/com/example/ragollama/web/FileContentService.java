package com.example.ragollama.web;

import com.example.ragollama.shared.exception.ResourceNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Сервис для безопасного чтения содержимого файлов.
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class FileContentService {

    private final FileMetadataRepository fileMetadataRepository;

    /**
     * Асинхронно извлекает содержимое нескольких файлов и объединяет его в одну строку.
     * @param fileIds Список ID файлов.
     * @return Mono с объединенным текстовым содержимым.
     */
    public Mono<String> getAggregatedContent(List<UUID> fileIds) {
        if (fileIds == null || fileIds.isEmpty()) {
            return Mono.empty();
        }
        String username = getCurrentUsername();

        return Flux.fromIterable(fileIds)
                .flatMap(fileId -> fileMetadataRepository.findById(fileId)
                        .switchIfEmpty(Mono.error(new ResourceNotFoundException("Файл с ID " + fileId + " не найден.")))
                        .flatMap(metadata -> {
                            if (!metadata.getUserName().equals(username)) {
                                return Mono.error(new SecurityException("Доступ к файлу " + fileId + " запрещен."));
                            }
                            return readFileContent(Path.of(metadata.getFilePath()))
                                    .map(content -> formatFileContent(metadata.getFileName(), content));
                        }))
                .collect(Collectors.joining("\n\n---\n\n"));
    }

    private Mono<String> readFileContent(Path path) {
        return Mono.fromCallable(() -> {
            try {
                return Files.readString(path);
            } catch (IOException e) {
                log.error("Не удалось прочитать файл: {}", path, e);
                throw new RuntimeException("Ошибка чтения файла: " + path.getFileName(), e);
            }
        }).subscribeOn(Schedulers.boundedElastic());
    }

    private String formatFileContent(String fileName, String content) {
        return String.format("<document name=\"%s\">\n%s\n</document>", fileName, content);
    }

    private String getCurrentUsername() {
        return "default-user";
    }
}
