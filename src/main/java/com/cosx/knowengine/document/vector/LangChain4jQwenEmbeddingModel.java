package com.cosx.knowengine.document.vector;

import com.cosx.knowengine.document.vector.config.QwenProperties;
import com.cosx.knowengine.exception.BusinessException;
import dev.langchain4j.data.embedding.Embedding;
import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.model.embedding.EmbeddingModel;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.List;

@Component
@RequiredArgsConstructor
public class LangChain4jQwenEmbeddingModel implements DocumentEmbeddingModel {

    private final ObjectProvider<EmbeddingModel> embeddingModelProvider;
    private final QwenProperties properties;

    @Override
    public List<float[]> embedAll(List<String> contents) {
        if (contents.isEmpty()) {
            return List.of();
        }
        if (!properties.isEnabled() || !StringUtils.hasText(properties.getApiKey())) {
            throw BusinessException.unavailable(50320, "Qwen 向量模型尚未启用或未配置 API Key");
        }
        EmbeddingModel embeddingModel = embeddingModelProvider.getIfAvailable();
        if (embeddingModel == null) {
            throw BusinessException.unavailable(50321, "LangChain4j Qwen 向量模型不可用");
        }

        List<float[]> result = new ArrayList<>(contents.size());
        int batchSize = Math.max(1, properties.getMaxBatchSize());
        for (int start = 0; start < contents.size(); start += batchSize) {
            int end = Math.min(start + batchSize, contents.size());
            List<TextSegment> segments = contents.subList(start, end).stream()
                    .map(TextSegment::from)
                    .toList();
            List<Embedding> embeddings = embeddingModel.embedAll(segments).content();
            if (embeddings.size() != segments.size()) {
                throw BusinessException.upstream(50220, "Qwen 返回的向量数量与分段数量不一致");
            }
            for (Embedding embedding : embeddings) {
                float[] vector = embedding.vector();
                if (vector.length != properties.getDimensions()) {
                    throw BusinessException.upstream(50221, "Qwen 返回的向量维度与配置不一致");
                }
                result.add(vector);
            }
        }
        return result;
    }

    @Override
    public String modelKey() {
        return properties.modelKey();
    }

    @Override
    public int dimensions() {
        return properties.getDimensions();
    }
}
