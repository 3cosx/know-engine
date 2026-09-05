package com.cosx.knowengine;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;

@MapperScan({"com.cosx.knowengine.user.mapper", "com.cosx.knowengine.document.persistence.mapper"})
@EnableAsync
@EnableScheduling
@ConfigurationPropertiesScan
@SpringBootApplication
public class KnowEngineApplication {

    public static void main(String[] args) {
        SpringApplication.run(KnowEngineApplication.class, args);
    }
}
