package com.cosx.knowengine.document.storage.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.Duration;

@Data
@ConfigurationProperties(prefix = "know-engine.minio")
public class MinioProperties {

    private String endpoint = "http://localhost:9000";

    private String accessKey = "minioadmin";

    private String secretKey = "minioadmin";

    private String bucket = "know-engine";

    private Duration presignedExpiry = Duration.ofMinutes(30);
}
