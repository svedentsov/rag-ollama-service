package com.example.ragollama.shared.util;

import org.springframework.ai.vectorstore.filter.Filter;
import org.springframework.util.DigestUtils;
import java.nio.charset.StandardCharsets;

/**
 * Вспомогательный класс для генерации ключей кэша для объектов Filter.Expression.
 * <p>
 * Так как {@link org.springframework.ai.vectorstore.filter.Filter.Expression} не имеет
 * стабильного метода `toString()` или `hashCode()`, стандартный генератор ключей
 * Spring Cache не может надежно кэшировать результаты. Этот класс решает проблему,
 * создавая детерминированную строковую репрезентацию фильтра и хешируя ее.
 */
public final class FilterExpressionKeyHelper {

    private FilterExpressionKeyHelper() {
        // Утилитарный класс не должен иметь публичного конструктора
    }

    /**
     * Генерирует стабильный ключ для объекта Filter.Expression.
     *
     * @param expression Фильтр для генерации ключа. Может быть {@code null}.
     * @return MD5-хеш от строкового представления фильтра или "null_filter", если фильтр отсутствует.
     */
    public static String generateKey(Filter.Expression expression) {
        if (expression == null) {
            return "null_filter";
        }
        String stringRepresentation = expressionToString(expression);
        return DigestUtils.md5DigestAsHex(stringRepresentation.getBytes(StandardCharsets.UTF_8));
    }

    /**
     * Рекурсивно преобразует объект фильтра в каноническую строку.
     *
     * @param expr Фильтр для преобразования.
     * @return Строковое представление фильтра.
     */
    private static String expressionToString(Filter.Expression expr) {
        StringBuilder sb = new StringBuilder();
        sb.append(expr.type()).append("(");
        if (expr.left() instanceof Filter.Key key) {
            sb.append(key.key());
        } else if (expr.left() instanceof Filter.Expression leftExpr) {
            sb.append(expressionToString(leftExpr));
        }

        sb.append(",");

        if (expr.right() instanceof Filter.Value val) {
            sb.append(val.value());
        } else if (expr.right() instanceof Filter.Expression rightExpr) {
            sb.append(expressionToString(rightExpr));
        }
        sb.append(")");
        return sb.toString();
    }
}
