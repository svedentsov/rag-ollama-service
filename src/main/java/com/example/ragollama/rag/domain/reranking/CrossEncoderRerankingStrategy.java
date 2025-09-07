package com.example.ragollama.rag.domain.reranking;

import ai.djl.huggingface.tokenizers.Encoding;
import ai.djl.huggingface.tokenizers.HuggingFaceTokenizer;
import ai.djl.inference.Predictor;
import ai.djl.ndarray.NDArray;
import ai.djl.ndarray.NDList;
import ai.djl.ndarray.NDManager;
import ai.djl.translate.Batchifier;
import ai.djl.translate.TranslateException;
import ai.djl.translate.Translator;
import ai.djl.translate.TranslatorContext;

import java.util.Objects;

public class CrossEncoderRerankingStrategy {

    private final Predictor<String[], Float> predictor;
    private final HuggingFaceTokenizer tokenizer;

    /**
     * Конструктор.
     *
     * @param predictor Predictor, настроенный для модели cross-encoder, который принимает String[] (pair) и возвращает Float
     * @param tokenizer HuggingFaceTokenizer — тот же tokenizer, который использовался при создании predictor'а (рекомендовано)
     */
    public CrossEncoderRerankingStrategy(Predictor<String[], Float> predictor,
                                         HuggingFaceTokenizer tokenizer) {
        this.predictor = Objects.requireNonNull(predictor, "predictor must not be null");
        this.tokenizer = Objects.requireNonNull(tokenizer, "tokenizer must not be null");
    }

    /**
     * Принимает массив пар: pairs[i] = new String[]{query, passage}
     * Возвращает массив скорoв той же длины.
     *
     * @param pairs пары [query, passage]
     * @return scores
     * @throws TranslateException при ошибке предсказания
     */
    public float[] rerank(String[][] pairs) throws TranslateException {
        if (pairs == null) {
            return new float[0];
        }
        float[] scores = new float[pairs.length];
        for (int i = 0; i < pairs.length; i++) {
            String[] pair = pairs[i];
            if (pair == null || pair.length < 2) {
                scores[i] = 0f;
                continue;
            }
            // predictor принимает одну пару (String[]) и возвращает Float
            Float s = predictor.predict(pair);
            scores[i] = (s != null) ? s : 0f;
        }
        return scores;
    }

    /**
     * Полная реализация Translator для cross-encoder:
     * - processInput: использует HuggingFaceTokenizer.encode(first, second)
     * и формирует NDList: [input_ids, attention_mask] (+ token_type_ids если есть)
     * - processOutput: извлекает из NDList logits/score и возвращает single Float score.
     * <p>
     * Реализация универсальна для моделей, которые возвращают:
     * - одиночный логит (shape [1,1]) -> возвращаем это значение
     * - два логита (shape [1,2]) -> применяем softmax и возвращаем вероятность положительного класса (index 1)
     * <p>
     * Замечание: для конкретной модели может потребоваться другой постпроцесс (например, использование pooled output).
     */
    public static class CrossEncoderTranslator implements Translator<String[], Float> {

        private final HuggingFaceTokenizer tokenizer;
        private final int maxLength;

        /**
         * @param tokenizer HuggingFaceTokenizer (например, HuggingFaceTokenizer.newInstance(modelId))
         * @param maxLength макс. длина (используется для pad/truncate при необходимости). Если <=0 — используется поведение токенайзера по умолчанию.
         */
        public CrossEncoderTranslator(HuggingFaceTokenizer tokenizer, int maxLength) {
            this.tokenizer = Objects.requireNonNull(tokenizer, "tokenizer must not be null");
            this.maxLength = maxLength;
        }

        @Override
        public NDList processInput(TranslatorContext ctx, String[] input) throws Exception {
            if (input == null || input.length < 2) {
                throw new IllegalArgumentException("input must be a String[] with two elements: {first, second}");
            }
            String first = input[0];
            String second = input[1];

            // Выполняем токенизацию пары (включая спец. токены, разделители и т.д.)
            // encode(first, second) возвращает Encoding с ids, attention mask и (опционально) type ids.
            Encoding encoding;
            if (maxLength > 0) {
                // есть перегрузка encode(String, String, boolean) — но тут используем простую encode
                // если нужно управление padding/truncation — конфигурировать токенайзер заранее через builder
                encoding = tokenizer.encode(first, second);
            } else {
                encoding = tokenizer.encode(first, second);
            }

            long[] ids = encoding.getIds(); // token ids
            long[] attention = encoding.getAttentionMask(); // attention mask
            long[] typeIds = encoding.getTypeIds(); // may be null (some tokenizers/models)

            NDManager mgr = ctx.getNDManager();

            // Создаём NDArrays и приводим к форме [1, seq_len]
            NDArray idsArray = mgr.create(ids).reshape(1, ids.length);
            NDArray attentionArray = mgr.create(attention).reshape(1, attention.length);

            NDList ndList = new NDList();
            ndList.add(idsArray);
            ndList.add(attentionArray);

            if (typeIds != null && typeIds.length == ids.length) {
                NDArray typeIdsArray = mgr.create(typeIds).reshape(1, typeIds.length);
                ndList.add(typeIdsArray);
            }

            return ndList;
        }

        @Override
        public Float processOutput(TranslatorContext ctx, NDList list) throws Exception {
            if (list == null || list.isEmpty()) {
                return 0f;
            }
            // Берём первый NDArray из списка — предположим, что он содержит логиты
            NDArray out = list.get(0);

            // Переносим значения в java-массив
            float[] arr = out.toFloatArray();

            if (arr.length == 0) {
                return 0f;
            } else if (arr.length == 1) {
                // Прямой логит/скор
                return arr[0];
            } else if (arr.length == 2) {
                // Два логита -> считаем softmax и возвращаем вероятность класса 1
                double e0 = Math.exp(arr[0]);
                double e1 = Math.exp(arr[1]);
                double sum = e0 + e1;
                if (sum == 0.0) {
                    return 0f;
                }
                double prob1 = e1 / sum;
                return (float) prob1;
            } else {
                // Если модель вернула вектор (например, pooled output), можно взять первый элемент как proxy
                return arr[0];
            }
        }

        @Override
        public Batchifier getBatchifier() {
            // null — мы не используем автоматический батчинг в Translator, batching может быть реализован извне
            return null;
        }
    }
}
