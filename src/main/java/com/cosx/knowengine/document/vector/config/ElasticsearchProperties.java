package com.cosx.knowengine.document.vector.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Data
@ConfigurationProperties(prefix = "know-engine.elasticsearch")
public class ElasticsearchProperties {

    private String endpoint = "http://localhost:9200";

    private String username;

    private String password;

    private String indexName = "know-document-segments-v1";

    private String indexSchemaVersion = "v1";
}
