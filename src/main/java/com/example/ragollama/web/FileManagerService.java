package com.example.ragollama.web;

import com.example.ragollama.indexing.IndexingPipelineService;
import com.example.ragollama.indexing.IndexingRequest;
import com.example.ragollama.shared.exception.AccessDeniedException;
import com.example.ragollama.shared.exception.ProcessingException;
import com.example.ragollama.shared.exception.ResourceNotFoundException;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.http.codec.multipart.FilePart;
import org.springframework.r2dbc.core.DatabaseClient;
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
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * Сервисный слой для инкапсуляции бизнес-логики управления файлами.
 * <p>
 * Метод `uploadFile` теперь является идемпотентным: он обновляет существующий
 * файл или создает новый, если он не существует, предотвращая ошибки дубликатов.
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class FileManagerService {

    private final FileMetadataRepository fileMetadataRepository;
    private final FileManagerProperties properties;
    private final IndexingPipelineService indexingPipelineService;
    private final DatabaseClient databaseClient;

    private Path rootLocation;
    private static final Set<String> ALLOWED_SORT_COLUMNS =
            Set.of("id", "file_name", "file_size", "created_at", "updated_at");

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

    /**
     * Идемпотентно загружает или обновляет файл.
     * <p>
     * Сначала ищет файл по имени и пользователю. Если файл найден, он обновляется.
     * Если нет — создается новая запись. Все операции выполняются в одной транзакции.
     *
     * @param filePart Загружаемый файл.
     * @return Mono с метаданными сохраненного или обновленного файла.
     */
    @Transactional
    public Mono<FileMetadata> uploadFile(FilePart filePart) {
        String username = getCurrentUsername();
        String originalFilename = filePart.filename();
        validateFile(filePart);

        return fileMetadataRepository.findByUserNameAndFileName(username, originalFilename)
                .flatMap(existingMetadata -> {
                    log.info("Обнаружен существующий файл '{}'. Выполняется обновление.", originalFilename);
                    return updateExistingFile(filePart, existingMetadata);
                })
                .switchIfEmpty(Mono.defer(() -> {
                    log.info("Файл '{}' не найден. Выполняется создание нового файла.", originalFilename);
                    return createNewFile(filePart, username, originalFilename);
                }));
    }

    private Mono<FileMetadata> updateExistingFile(FilePart filePart, FileMetadata metadata) {
        Path destinationFile = Paths.get(metadata.getFilePath());
        return saveAndIndexFile(filePart, destinationFile, metadata);
    }

    private Mono<FileMetadata> createNewFile(FilePart filePart, String username, String originalFilename) {
        String storedFileName = UUID.randomUUID() + "-" + originalFilename;
        Path destinationFile = this.rootLocation.resolve(storedFileName).normalize();
        FileMetadata metadata = FileMetadata.builder()
                .userName(username)
                .fileName(originalFilename)
                .filePath(destinationFile.toString())
                .mimeType(filePart.headers().getContentType().toString())
                .build();
        return saveAndIndexFile(filePart, destinationFile, metadata);
    }

    private Mono<FileMetadata> saveAndIndexFile(FilePart filePart, Path destination, FileMetadata metadata) {
        return filePart.transferTo(destination)
                .then(Mono.fromCallable(() -> Files.readString(destination, StandardCharsets.UTF_8))
                        .subscribeOn(Schedulers.boundedElastic())
                )
                .flatMap(fileContent -> {
                    metadata.setFileSize((long) fileContent.getBytes(StandardCharsets.UTF_8).length);
                    // updatedAt будет обновлен автоматически через @LastModifiedDate

                    return fileMetadataRepository.save(metadata)
                            .doOnSuccess(savedMetadata -> {
                                log.info("Файл {} сохранен/обновлен на диске и в БД. Запуск переиндексации...", savedMetadata.getFileName());
                                IndexingRequest indexingRequest = new IndexingRequest(
                                        savedMetadata.getId().toString(),
                                        savedMetadata.getFileName(),
                                        fileContent,
                                        Map.of("doc_type", "user_file", "user", savedMetadata.getUserName())
                                );
                                indexingPipelineService.process(indexingRequest).subscribe(
                                        null,
                                        error -> log.error("Ошибка при фоновой индексации файла {}", savedMetadata.getFileName(), error)
                                );
                            });
                });
    }

    @Transactional(readOnly = true)
    public Mono<Page<FileMetadata>> getFilesForUser(Pageable pageable, String query) {
        String username = getCurrentUsername();
        String sortProperty = pageable.getSort().get()
                .map(org.springframework.data.domain.Sort.Order::getProperty)
                .findFirst()
                .orElse("createdAt");

        String safeSortProperty = ALLOWED_SORT_COLUMNS.contains(sortProperty) ? sortProperty : "created_at";
        String dbSortColumn = toSnakeCase(safeSortProperty);

        String sortDirection = pageable.getSort().get()
                .map(o -> o.getDirection().name())
                .findFirst()
                .orElse("DESC");

        String sql = String.format(
                "SELECT * FROM file_metadata WHERE user_name = :userName AND file_name ILIKE :query ORDER BY %s %s LIMIT :size OFFSET :offset",
                dbSortColumn, sortDirection
        );

        Flux<FileMetadata> filesFlux = databaseClient.sql(sql)
                .bind("userName", username)
                .bind("query", "%" + query + "%")
                .bind("size", pageable.getPageSize())
                .bind("offset", pageable.getOffset())
                .map(row -> FileMetadata.builder()
                        .id(row.get("id", UUID.class))
                        .userName(row.get("user_name", String.class))
                        .fileName(row.get("file_name", String.class))
                        .filePath(row.get("file_path", String.class))
                        .mimeType(row.get("mime_type", String.class))
                        .fileSize(row.get("file_size", Long.class))
                        .createdAt(row.get("created_at", OffsetDateTime.class))
                        .updatedAt(row.get("updated_at", OffsetDateTime.class))
                        .build())
                .all();

        Mono<Long> countMono = fileMetadataRepository.countByUserNameWithFilter(username, "%" + query + "%");

        return Mono.zip(filesFlux.collectList(), countMono)
                .map(tuple -> new PageImpl<>(tuple.getT1(), pageable, tuple.getT2()));
    }

    @Transactional
    public Mono<Void> deleteFiles(List<UUID> fileIds) {
        String username = getCurrentUsername();
        return fileMetadataRepository.findAllById(fileIds)
                .collectList()
                .flatMap(metadataList -> {
                    if (metadataList.stream().anyMatch(meta -> !meta.getUserName().equals(username))) {
                        return Mono.error(new AccessDeniedException("Доступ запрещен."));
                    }

                    Flux<Void> deleteFromDiskFlux = Flux.fromIterable(metadataList)
                            .flatMap(metadata -> Mono.fromRunnable(() -> {
                                try {
                                    Files.deleteIfExists(Paths.get(metadata.getFilePath()));
                                } catch (IOException e) {
                                    throw new ProcessingException("Ошибка удаления файла с диска", e);
                                }
                            }).subscribeOn(Schedulers.boundedElastic()).then());

                    List<String> documentIds = metadataList.stream().map(m -> m.getId().toString()).toList();
                    Mono<Long> deleteFromVectorStoreMono = indexingPipelineService.delete(documentIds);

                    Mono<Void> deleteFromDbMono = fileMetadataRepository.deleteAllByIdIn(fileIds);

                    return Mono.when(deleteFromDiskFlux, deleteFromVectorStoreMono, deleteFromDbMono).then();
                });
    }

    @Transactional(readOnly = true)
    public Mono<String> getFileContent(UUID fileId) {
        String username = getCurrentUsername();
        return fileMetadataRepository.findById(fileId)
                .switchIfEmpty(Mono.error(new ResourceNotFoundException("Файл с ID " + fileId + " не найден.")))
                .flatMap(metadata -> {
                    if (!metadata.getUserName().equals(username)) {
                        return Mono.error(new AccessDeniedException("Доступ запрещен."));
                    }
                    return readFileContent(Path.of(metadata.getFilePath()));
                });
    }

    private void validateFile(FilePart filePart) {
        String mimeType = filePart.headers().getContentType() != null ? filePart.headers().getContentType().toString() : "";
        if (!properties.getAllowedMimeTypes().contains(mimeType)) {
            throw new IllegalArgumentException("Недопустимый тип файла: " + mimeType);
        }
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

    private String getCurrentUsername() {
        return "default-user";
    }

    private String toSnakeCase(String camelCase) {
        if (camelCase == null) return null;
        return camelCase.replaceAll("([a-z])([A-Z]+)", "$1_$2").toLowerCase();
    }
}
