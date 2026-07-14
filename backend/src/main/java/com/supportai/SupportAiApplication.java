package com.supportai;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import com.supportai.config.StorageProperties;

@SpringBootApplication
@EnableConfigurationProperties(StorageProperties.class)
public class SupportAiApplication {

    public static void main(String[] args) {
        SpringApplication.run(SupportAiApplication.class, args);
    }
}
