package com.example.ragollama.orchestration.validation;

import com.example.ragollama.orchestration.dto.UniversalRequest;
import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import org.springframework.util.StringUtils;

/**
 * Реализация кастомного валидатора для {@link UniversalRequest}.
 * Проверяет выполнение бизнес-правила: "либо query, либо context должны быть не пустыми".
 */
public class UniversalRequestValidator implements ConstraintValidator<ValidRequestPayload, UniversalRequest> {

    /**
     * Проверяет валидность объекта UniversalRequest.
     *
     * @param request объект для валидации.
     * @param context контекст, в котором происходит валидация.
     * @return {@code true}, если запрос содержит либо query, либо context, иначе {@code false}.
     */
    @Override
    public boolean isValid(UniversalRequest request, ConstraintValidatorContext context) {
        if (request == null) {
            return true; // Проверку на null лучше делать через @NotNull на параметре контроллера
        }

        boolean hasQuery = StringUtils.hasText(request.query());
        boolean hasContext = StringUtils.hasText(request.context());

        return hasQuery || hasContext;
    }
}
