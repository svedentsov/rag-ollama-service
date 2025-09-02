package com.example.ragollama.agent.executive.model;

import com.example.ragollama.agent.strategy.model.PortfolioStrategyReport;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Data;

/**
 * DTO для финального, агрегированного дашборда "Executive Command Center".
 *
 * @param portfolioHealth     Отчет о техническом здоровье и рисках портфеля.
 * @param engineeringVelocity Отчет о скорости и эффективности процессов разработки.
 * @param productStrategy     Отчет о рыночных возможностях и продуктовой стратегии.
 * @param financialRoiReport  Отчет о затратах и рентабельности инвестиций.
 * @param architecturalHealth Отчет о долгосрочном здоровье архитектуры.
 */
@Schema(description = "Сводный дашборд для руководства (Executive Command Center)")
@Data
@Builder
public class ExecutiveDashboard {

    @Schema(description = "Отчет о техническом здоровье и рисках портфеля (AI CTO)")
    private PortfolioStrategyReport portfolioHealth;

    @Schema(description = "Отчет о скорости и эффективности процессов разработки (AI VP of Engineering)")
    private EngineeringEfficiencyReport engineeringVelocity;

    @Schema(description = "Отчет о рыночных возможностях и продуктовой стратегии (AI CPO)")
    private ProductStrategyReport productStrategy;

    @Schema(description = "Отчет о затратах и рентабельности инвестиций (AI CFO)")
    private FinancialRoiReport financialRoiReport;

    @Schema(description = "Отчет о долгосрочном здоровье архитектуры (AI Chief Architect)")
    private ArchitecturalHealthReport architecturalHealth;
}
