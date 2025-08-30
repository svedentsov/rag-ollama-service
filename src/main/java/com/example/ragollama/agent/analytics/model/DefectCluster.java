package com.example.ragollama.agent.analytics.model;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.RequiredArgsConstructor;

import java.util.List;

/**
 * DTO для представления одного кластера семантически похожих дефектов.
 */
@Schema(description = "Кластер семантически похожих дефектов")
@Data
@RequiredArgsConstructor
public class DefectCluster {
    private String summary = "Анализ...";
    private final int defectCount;
    private final List<Defect> defects;
}
