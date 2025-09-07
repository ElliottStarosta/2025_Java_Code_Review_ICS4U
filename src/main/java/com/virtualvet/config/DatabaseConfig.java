package com.virtualvet.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Bean;
import javax.sql.DataSource;
import org.springframework.boot.jdbc.DataSourceBuilder;
import org.springframework.beans.factory.annotation.Value;

@Configuration
public class DatabaseConfig {

    @Value("${spring.datasource.url:jdbc:h2:mem:vetchat}")
    private String datasourceUrl;
    
    @Value("${spring.datasource.username:sa}")
    private String datasourceUsername;
    
    @Value("${spring.datasource.password:}")
    private String datasourcePassword;
    
    @Bean
    public DataSource dataSource() {
        return DataSourceBuilder.create()
                .driverClassName("org.h2.Driver")
                .url(datasourceUrl)
                .username(datasourceUsername)
                .password(datasourcePassword)
                .build();
    }
}