package com.example.ragollama.rag.embedding;

import com.example.ragollama.shared.util.EmbeddingUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.document.Document;
import org.springframework.ai.embedding.Embedding;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.ai.embedding.EmbeddingRequest;
import org.springframework.ai.embedding.EmbeddingResponse;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Декоратор над стандартным {@link EmbeddingModel}, который добавляет
 * шаг L2-нормализации ко всем сгенерированным векторам.
 * Эта версия соответствует API Spring AI 1.0.x, использующему float[].
 */
@Slf4j
@RequiredArgsConstructor
public class NormalizingEmbeddingModel implements EmbeddingModel {

    private final EmbeddingModel delegate;

    @Override
    public EmbeddingResponse call(EmbeddingRequest request) {
        EmbeddingResponse originalResponse = delegate.call(request);

        List<Embedding> normalizedEmbeddings = originalResponse.getResults().stream()
                .map(this::normalizeEmbedding)
                .collect(Collectors.toList());

        log.trace("Выполнена L2-нормализация для {} эмбеддингов.", normalizedEmbeddings.size());

        return new EmbeddingResponse(normalizedEmbeddings, originalResponse.getMetadata());
    }

    /**
     * Вспомогательный метод для нормализации одного эмбеддинга.
     */
    private Embedding normalizeEmbedding(Embedding original) {
        float[] output = original.getOutput();
        float[] toNormalize = output.clone();
        float[] normalizedVector = EmbeddingUtils.normalize(toNormalize);
        return new Embedding(normalizedVector, original.getIndex(), original.getMetadata());
    }

    @Override
    public float[] embed(Document document) {
        return EmbeddingUtils.normalize(delegate.embed(document));
    }

    @Override
    public float[] embed(String text) {
        return EmbeddingUtils.normalize(delegate.embed(text));
    }

    @Override
    public List<float[]> embed(List<String> texts) {
        return delegate.embed(texts).stream()
                .map(EmbeddingUtils::normalize)
                .collect(Collectors.toList());
    }

    @Override
    public int dimensions() {
        return delegate.dimensions();
    }
}
