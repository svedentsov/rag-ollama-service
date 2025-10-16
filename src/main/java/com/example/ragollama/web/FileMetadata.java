package com.example.ragollama.web;

import jakarta.validation.constraints.NotNull;
import lombok.*;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;

import java.time.OffsetDateTime;
import java.util.UUID;

/**
 * Сущность для хранения метаданных о загруженном файле.
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Table("file_metadata")
public class FileMetadata {

    @Id
    private UUID id;

    @NotNull
    @Column("user_name")
    private String userName;

    @NotNull
    @Column("file_name")
    private String fileName;

    @NotNull
    @Column("file_path")
    private String filePath;

    @NotNull
    @Column("mime_type")
    private String mimeType;

    @NotNull
    @Column("file_size")
    private Long fileSize;

    @CreatedDate
    @Column("created_at")
    private OffsetDateTime createdAt;

    @LastModifiedDate
    @Column("updated_at")
    private OffsetDateTime updatedAt;
}
