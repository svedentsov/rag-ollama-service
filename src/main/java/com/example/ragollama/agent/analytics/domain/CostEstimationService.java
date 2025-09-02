package com.example.ragollama.agent.analytics.domain;

import com.example.ragollama.agent.config.CostProperties;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

/**
 * Детерминированный сервис для расчета стоимости на основе бизнес-метрик.
 * <p>
 * Инкапсулирует бизнес-логику и предположения о стоимости,
 * загруженные из конфигурационного файла {@link CostProperties}. Этот сервис
 * не использует AI, а выполняет простые, предсказуемые расчеты.
 */
@Service
@RequiredArgsConstructor
public class CostEstimationService {

    private final CostProperties properties;

    /**
     * Рассчитывает общую стоимость исправления (рефакторинга).
     *
     * @param devHours Оценочное количество часов разработчика, полученное от LLM.
     * @return Стоимость в денежном эквиваленте.
     */
    public double calculateRemediationCost(int devHours) {
        return devHours * properties.perDevHour();
    }

    /**
     * Рассчитывает общую стоимость бездействия за один месяц.
     *
     * @param supportTickets Оценочное количество тикетов в поддержку, полученное от LLM.
     * @return Стоимость в денежном эквиваленте.
     */
    public double calculateInactionCost(int supportTickets) {
        return supportTickets * properties.perSupportTicket();
    }
}
