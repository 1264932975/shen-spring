package com.shen.framework.config;

import okhttp3.ConnectionPool;
import okhttp3.OkHttpClient;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.OkHttp3ClientHttpRequestFactory;
import org.springframework.web.client.RestTemplate;

import java.time.Duration;

/**
 * RestTemplate 配置（底层使用 OkHttp）
 */
@Configuration
public class RestTemplateConfig {

    @Bean
    public RestTemplate restTemplate() {
        OkHttpClient okHttpClient = new OkHttpClient.Builder()
                // 连接池：最大空闲连接数 10，空闲存活 5 分钟
                .connectionPool(new ConnectionPool(10, Duration.ofMinutes(5)))
                .build();

        return new RestTemplate(new OkHttp3ClientHttpRequestFactory(okHttpClient));
    }
}