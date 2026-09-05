package com.cosx.knowengine.document.vector.config;

import org.apache.http.HttpHeaders;
import org.apache.http.HttpHost;
import org.apache.http.message.BasicHeader;
import org.elasticsearch.client.RestClient;
import org.elasticsearch.client.RestClientBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.util.StringUtils;

import java.nio.charset.StandardCharsets;
import java.util.Base64;

@Configuration(proxyBeanMethods = false)
public class ElasticsearchConfiguration {

    @Bean(destroyMethod = "close")
    RestClient elasticsearchRestClient(ElasticsearchProperties properties) {
        RestClientBuilder builder = RestClient.builder(HttpHost.create(properties.getEndpoint()));
        if (StringUtils.hasText(properties.getUsername())) {
            String credentials = properties.getUsername() + ":" + properties.getPassword();
            String encoded = Base64.getEncoder().encodeToString(credentials.getBytes(StandardCharsets.UTF_8));
            builder.setDefaultHeaders(new BasicHeader[]{
                    new BasicHeader(HttpHeaders.AUTHORIZATION, "Basic " + encoded)
            });
        }
        return builder.build();
    }
}
