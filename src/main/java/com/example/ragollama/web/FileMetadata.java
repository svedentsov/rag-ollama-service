package com.example.ragollama.web;

import com.fasterxml.jackson.annotation.JsonProperty;
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
    @JsonProperty("userName")
    private String userName;

    @NotNull
    @Column("file_name")
    @JsonProperty("fileName")
    private String fileName;

    @NotNull
    @Column("file_path")
    @JsonProperty("filePath")
    private String filePath;

    @NotNull
    @Column("mime_type")
    @JsonProperty("mimeType")
    private String mimeType;

    @NotNull
    @Column("file_size")
    @JsonProperty("fileSize")
    private Long fileSize;

    @CreatedDate
    @Column("created_at")
    @JsonProperty("createdAt")
    private OffsetDateTime createdAt;

    @LastModifiedDate
    @Column("updated_at")
    @JsonProperty("updatedAt")
    private OffsetDateTime updatedAt;
}
