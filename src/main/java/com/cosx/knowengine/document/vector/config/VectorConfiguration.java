package com.cosx.knowengine.document.vector.config;

import dev.langchain4j.model.embedding.EmbeddingModel;
import dev.langchain4j.model.openai.OpenAiEmbeddingModel;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration(proxyBeanMethods = false)
public class VectorConfiguration {

    @Bean
    @ConditionalOnProperty(prefix = "know-engine.qwen", name = "enabled", havingValue = "true")
    EmbeddingModel qwenEmbeddingModel(QwenProperties properties) {
        return OpenAiEmbeddingModel.builder()
                .baseUrl(properties.getBaseUrl())
                .apiKey(properties.getApiKey())
                .modelName(properties.getModelName())
                .dimensions(properties.getDimensions())
                .timeout(properties.getTimeout())
                .build();
    }
}
