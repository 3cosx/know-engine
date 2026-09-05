package com.cosx.knowengine;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@MapperScan("com.cosx.knowengine.mapper")
@SpringBootApplication
public class KnowEngineApplication {

    public static void main(String[] args) {
        SpringApplication.run(KnowEngineApplication.class, args);
    }
}
