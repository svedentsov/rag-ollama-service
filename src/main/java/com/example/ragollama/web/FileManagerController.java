package com.example.ragollama.web;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.http.codec.multipart.FilePart;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

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
     * Получает список метаданных всех файлов для текущего пользователя.
     *
     * @return Flux с DTO файлов.
     */
    @GetMapping
    @Operation(summary = "Получить список файлов")
    public Flux<FileMetadata> listFiles() {
        return fileManagerService.getFilesForUser();
    }

    /**
     * Загружает новый файл.
     *
     * @param filePart Mono с файлом из multipart-запроса.
     * @return Mono с DTO созданного файла и статусом 201.
     */
    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @Operation(summary = "Загрузить новый файл")
    public Mono<ResponseEntity<FileMetadata>> uploadFile(@RequestPart("file") Mono<FilePart> filePart) {
        return filePart
                .flatMap(fileManagerService::uploadFile)
                .map(metadata -> ResponseEntity.status(HttpStatus.CREATED).body(metadata));
    }

    /**
     * Удаляет файл по его ID.
     *
     * @param fileId ID файла.
     * @return Mono с ResponseEntity со статусом 204.
     */
    @DeleteMapping("/{fileId}")
    @Operation(summary = "Удалить файл")
    public Mono<ResponseEntity<Void>> deleteFile(@PathVariable UUID fileId) {
        return fileManagerService.deleteFile(fileId)
                .thenReturn(ResponseEntity.noContent().build());
    }
}
