package com.example.ragollama.shared.security;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.vectorstore.filter.Filter;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * Сервис для централизованного управления контролем доступа к данным.
 * <p>
 * Отвечает за создание динамических фильтров для векторного хранилища
 * на основе прав и ролей текущего аутентифицированного пользователя.
 * Это ядро механизма Access Policy Enforcement.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AccessControlService {

    private static final String ALLOWED_ROLES_KEY = "metadata.allowedRoles";
    private static final String IS_PUBLIC_KEY = "metadata.isPublic";

    /**
     * Создает выражение фильтра для Vector Store на основе контекста безопасности.
     * Логика фильтрации:
     * 1. Если пользователь не аутентифицирован, он может видеть только публичные документы (`isPublic = true`).
     * 2. Если пользователь аутентифицирован, он может видеть публичные документы ИЛИ документы,
     * в метаданных `allowedRoles` которых содержится хотя бы одна из его ролей.
     *
     * @return {@link Filter.Expression}, готовый для использования в {@link org.springframework.ai.vectorstore.SearchRequest}.
     */
    public Filter.Expression buildAccessFilter() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        Filter.Expression publicDocsFilter = new Filter.Expression(Filter.ExpressionType.EQ, new Filter.Key(IS_PUBLIC_KEY), new Filter.Value(true));

        if (authentication == null || !authentication.isAuthenticated() || "anonymousUser".equals(authentication.getPrincipal())) {
            log.trace("Пользователь не аутентифицирован. Применен фильтр только для публичных документов.");
            return publicDocsFilter;
        }

        List<String> userRoles = authentication.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .toList();

        if (userRoles.isEmpty()) {
            log.trace("У аутентифицированного пользователя нет ролей. Применен фильтр только для публичных документов.");
            return publicDocsFilter;
        }

        log.debug("Создание фильтра для пользователя с ролями: {}", userRoles);
        Filter.Expression roleBasedFilter = new Filter.Expression(Filter.ExpressionType.IN, new Filter.Key(ALLOWED_ROLES_KEY), new Filter.Value(userRoles));
        // Комбинированный фильтр: (isPublic = true) OR (allowedRoles IN ['ROLE_USER', ...])
        return new Filter.Expression(Filter.ExpressionType.OR, publicDocsFilter, roleBasedFilter);
    }
}
