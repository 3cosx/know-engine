package com.cosx.knowengine.document.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Data
@ConfigurationProperties(prefix = "know-engine.document")
public class DocumentProperties {

    private long maxFileSize = 50L * 1024 * 1024;

    private int segmentSize = 500;

    private int segmentOverlap = 50;
}
