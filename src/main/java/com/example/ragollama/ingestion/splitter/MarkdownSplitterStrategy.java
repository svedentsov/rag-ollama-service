package com.example.ragollama.ingestion.splitter;

import org.springframework.ai.document.Document;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.List;
import java.util.regex.Pattern;

@Component
@Order(20)
public class MarkdownSplitterStrategy implements DocumentSplitterStrategy {

    private static final Pattern MARKDOWN_HEADER_PATTERN = Pattern.compile("(?m)^#{1,3}\\s.*");

    @Override
    public boolean supports(Document document) {
        String source = (String) document.getMetadata().get("source");
        return source != null && source.toLowerCase().contains("confluence");
    }

    @Override
    public List<Document> split(Document document, SplitterConfig config) {
        return Arrays.stream(MARKDOWN_HEADER_PATTERN.split(document.getText()))
                .filter(chunk -> !chunk.isBlank())
                .map(chunkText -> new Document(chunkText, document.getMetadata()))
                .toList();
    }
}
