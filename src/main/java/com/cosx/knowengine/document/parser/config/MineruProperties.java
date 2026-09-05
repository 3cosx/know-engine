package com.cosx.knowengine.document.parser.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.Duration;

@Data
@ConfigurationProperties(prefix = "know-engine.mineru")
public class MineruProperties {

    private String baseUrl = "https://mineru.net";

    private String token;

    private Duration pollInterval = Duration.ofSeconds(5);

    private Duration timeout = Duration.ofMinutes(30);
}
