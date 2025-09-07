package com.virtualvet.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestTemplate;
import org.springframework.http.client.HttpComponentsClientHttpRequestFactory;
import org.springframework.boot.web.client.RestTemplateBuilder;
import java.time.Duration;
import java.io.File;

@Configuration
public class ApplicationConfig {

    @Bean
    public RestTemplate restTemplate(RestTemplateBuilder builder) {
        return builder
                .setConnectTimeout(Duration.ofSeconds(10))
                .setReadTimeout(Duration.ofSeconds(30))
                .requestFactory(HttpComponentsClientHttpRequestFactory.class)
                .build();
    }

    @Bean
    public String imageStoragePath() {
        String path = System.getProperty("IMAGE_STORAGE_PATH", "./uploads");
        File directory = new File(path);
        if (!directory.exists()) {
            directory.mkdirs();
        }
        return path;
    }
}
