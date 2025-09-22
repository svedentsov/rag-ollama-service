package com.example.ragollama.shared.util;

import lombok.experimental.UtilityClass;

/**
 * Утилитарный класс для математических операций над эмбеддингами.
 * Работает с примитивными массивами float[] для высокой производительности.
 */
@UtilityClass
public final class EmbeddingUtils {

    /**
     * Выполняет L2-нормализацию для вектора.
     * Нормализованный вектор имеет длину (magnitude) равную 1.
     * Этот метод изменяет исходный массив для экономии памяти.
     *
     * @param v Вектор для нормализации.
     * @return Тот же массив с нормализованными значениями.
     */
    public static float[] normalize(float[] v) {
        if (v == null || v.length == 0) {
            return v;
        }

        // 1. Вычисляем квадрат нормы (сумму квадратов компонентов)
        float normSq = 0.0f;
        for (float value : v) {
            normSq += value * value;
        }

        // 2. Если норма равна нулю, возвращаем исходный вектор
        if (normSq == 0.0f) {
            return v;
        }

        // 3. Вычисляем L2-норму
        final float norm = (float) Math.sqrt(normSq);

        // 4. Делим каждый компонент на норму
        for (int i = 0; i < v.length; i++) {
            v[i] /= norm;
        }
        return v;
    }
}
