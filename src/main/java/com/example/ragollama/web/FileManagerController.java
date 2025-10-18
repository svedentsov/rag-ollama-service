package com.example.ragollama.web;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.http.codec.multipart.FilePart;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.UUID;

/**
 * API-контроллер для управления файлами.
 */
@RestController
@RequestMapping("/api/v1/files")
@RequiredArgsConstructor
@Tag(name = "File Manager API", description = "API для загрузки и управления файлами")
public class FileManagerController {

    private final FileManagerService fileManagerService;

    /**
     * Получает страницу с метаданными файлов для текущего пользователя.
     *
     * @param page      Номер страницы (начиная с 0).
     * @param size      Размер страницы.
     * @param sort      Поле для сортировки.
     * @param direction Направление сортировки (ASC/DESC).
     * @param query     Поисковый запрос для фильтрации по имени.
     * @return Mono с пагинированным ответом.
     */
    @GetMapping
    @Operation(summary = "Получить список файлов с пагинацией, сортировкой и поиском")
    public Mono<Page<FileMetadata>> listFiles(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "createdAt") String sort,
            @RequestParam(defaultValue = "DESC") Sort.Direction direction,
            @RequestParam(defaultValue = "") String query
    ) {
        Pageable pageable = PageRequest.of(page, size, Sort.by(direction, sort));
        return fileManagerService.getFilesForUser(pageable, query);
    }

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @Operation(summary = "Загрузить новый файл")
    public Mono<ResponseEntity<FileMetadata>> uploadFile(@RequestPart("file") Mono<FilePart> filePart) {
        return filePart
                .flatMap(fileManagerService::uploadFile)
                .map(metadata -> ResponseEntity.status(HttpStatus.CREATED).body(metadata));
    }

    /**
     * Удаляет несколько файлов по их ID.
     *
     * @param fileIds Список ID файлов для удаления.
     * @return Mono с ResponseEntity со статусом 204.
     */
    @DeleteMapping
    @Operation(summary = "Удалить несколько файлов")
    public Mono<ResponseEntity<Void>> deleteFiles(@RequestBody List<UUID> fileIds) {
        return fileManagerService.deleteFiles(fileIds)
                .thenReturn(ResponseEntity.noContent().build());
    }

    @GetMapping(value = "/{fileId}/content", produces = MediaType.TEXT_PLAIN_VALUE)
    @Operation(summary = "Получить содержимое файла")
    public Mono<ResponseEntity<String>> getFileContent(@PathVariable UUID fileId) {
        return fileManagerService.getFileContent(fileId)
                .map(ResponseEntity::ok)
                .defaultIfEmpty(ResponseEntity.notFound().build());
    }
}
