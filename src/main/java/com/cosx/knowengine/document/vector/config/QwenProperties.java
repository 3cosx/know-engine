package com.cosx.knowengine.document.vector.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.Duration;

@Data
@ConfigurationProperties(prefix = "know-engine.qwen")
public class QwenProperties {

    private boolean enabled;

    private String apiKey;

    private String baseUrl = "https://dashscope.aliyuncs.com/compatible-mode/v1";

    private String modelName = "text-embedding-v4";

    private String modelVersion = "v1";

    private int dimensions = 1024;

    private int maxBatchSize = 10;

    private Duration timeout = Duration.ofSeconds(30);

    public String modelKey() {
        return modelName + ":" + modelVersion + ":" + dimensions;
    }
}
