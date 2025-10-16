package com.example.ragollama.web;

import com.example.ragollama.indexing.IndexingPipelineService;
import com.example.ragollama.indexing.IndexingRequest;
import com.example.ragollama.shared.exception.ProcessingException;
import com.example.ragollama.shared.exception.ResourceNotFoundException;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.codec.multipart.FilePart;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Map;
import java.util.UUID;

/**
 * Сервисный слой для инкапсуляции бизнес-логики управления файлами.
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class FileManagerService {

    private final FileMetadataRepository fileMetadataRepository;
    private final FileManagerProperties properties;
    private final IndexingPipelineService indexingPipelineService;
    private Path rootLocation;

    @PostConstruct
    public void init() {
        try {
            rootLocation = Paths.get(properties.getUploadDir()).toAbsolutePath().normalize();
            Files.createDirectories(rootLocation);
            log.info("Директория для хранения файлов инициализирована: {}", rootLocation);
        } catch (IOException e) {
            log.error("Не удалось инициализировать директорию для хранения файлов: {}", properties.getUploadDir(), e);
            throw new RuntimeException("Could not initialize file storage location", e);
        }
    }

    @Transactional
    public Mono<FileMetadata> uploadFile(FilePart filePart) {
        String username = getCurrentUsername();
        String originalFilename = filePart.filename();

        validateFile(filePart);

        String storedFileName = UUID.randomUUID() + "-" + originalFilename;
        Path destinationFile = this.rootLocation.resolve(storedFileName).normalize();

        return filePart.transferTo(destinationFile)
                .then(Mono.fromCallable(() -> Files.readString(destinationFile, StandardCharsets.UTF_8))
                        .subscribeOn(Schedulers.boundedElastic())
                )
                .flatMap(fileContent -> {
                    FileMetadata metadata = FileMetadata.builder()
                            .userName(username)
                            .fileName(originalFilename)
                            .filePath(destinationFile.toString())
                            .mimeType(filePart.headers().getContentType().toString())
                            .fileSize((long) fileContent.getBytes(StandardCharsets.UTF_8).length)
                            .build();

                    return fileMetadataRepository.save(metadata)
                            .doOnSuccess(savedMetadata -> {
                                log.info("Файл {} сохранен. Запуск асинхронной индексации...", savedMetadata.getFileName());
                                IndexingRequest indexingRequest = new IndexingRequest(
                                        savedMetadata.getId().toString(),
                                        savedMetadata.getFileName(),
                                        fileContent,
                                        Map.of("doc_type", "user_file", "user", username)
                                );
                                indexingPipelineService.process(indexingRequest).subscribe(
                                        null,
                                        error -> log.error("Ошибка при фоновой индексации файла {}", savedMetadata.getFileName(), error)
                                );
                            });
                });
    }

    @Transactional(readOnly = true)
    public Flux<FileMetadata> getFilesForUser() {
        return fileMetadataRepository.findByUserNameOrderByCreatedAtDesc(getCurrentUsername());
    }

    @Transactional
    public Mono<Void> deleteFile(UUID fileId) {
        String username = getCurrentUsername();
        return fileMetadataRepository.findById(fileId)
                .switchIfEmpty(Mono.error(new ResourceNotFoundException("Файл с ID " + fileId + " не найден.")))
                .flatMap(metadata -> {
                    if (!metadata.getUserName().equals(username)) {
                        return Mono.error(new SecurityException("Доступ запрещен."));
                    }
                    Mono<Void> deleteFromDiskMono = Mono.fromRunnable(() -> {
                        try {
                            Files.deleteIfExists(Paths.get(metadata.getFilePath()));
                        } catch (IOException e) {
                            throw new ProcessingException("Ошибка удаления файла с диска", e);
                        }
                    }).subscribeOn(Schedulers.boundedElastic()).then(); // ИСПРАВЛЕНИЕ: Добавляем .then()

                    IndexingRequest deleteRequest = new IndexingRequest(metadata.getId().toString(), null, "", null);
                    Mono<Void> deleteFromVectorStoreMono = indexingPipelineService.delete(deleteRequest);

                    Mono<Void> deleteFromDbMono = fileMetadataRepository.delete(metadata);

                    return Mono.when(deleteFromDiskMono, deleteFromVectorStoreMono, deleteFromDbMono);
                });
    }

    private void validateFile(FilePart filePart) {
        String mimeType = filePart.headers().getContentType() != null ? filePart.headers().getContentType().toString() : "";
        if (!properties.getAllowedMimeTypes().contains(mimeType)) {
            throw new IllegalArgumentException("Недопустимый тип файла: " + mimeType);
        }
    }

    private String getCurrentUsername() {
        return "default-user";
    }
}
