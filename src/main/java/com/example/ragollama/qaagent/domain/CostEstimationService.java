package com.example.ragollama.qaagent.domain;

import com.example.ragollama.qaagent.config.CostProperties;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

/**
 * Детерминированный сервис для расчета стоимости на основе бизнес-метрик.
 * <p>
 * Инкапсулирует бизнес-логику и предположения о стоимости,
 * загруженные из конфигурационного файла. Этот сервис не использует AI,
 * а выполняет простые, предсказуемые расчеты.
 */
@Service
@RequiredArgsConstructor
public class CostEstimationService {

    private final CostProperties properties;

    /**
     * Рассчитывает общую стоимость исправления (рефакторинга).
     *
     * @param devHours Оценочное количество часов разработчика.
     * @return Стоимость в денежном эквиваленте.
     */
    public double calculateRemediationCost(int devHours) {
        return devHours * properties.perDevHour();
    }

    /**
     * Рассчитывает общую стоимость бездействия за один месяц.
     * <p>
     * В более сложной модели здесь также могли бы учитываться
     * `potentialUserImpactPercentage` и `perLostUser`.
     *
     * @param supportTickets Оценочное количество тикетов в поддержку.
     * @return Стоимость в денежном эквиваленте.
     */
    public double calculateInactionCost(int supportTickets) {
        return supportTickets * properties.perSupportTicket();
    }
}
