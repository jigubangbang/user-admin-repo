package com.jigubangbang.payment_service.config;

import lombok.Getter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.reactive.function.client.WebClient;

@Getter
@Configuration
public class PortoneConfig {

    // 이 부분은 application.properties에서 값을 읽어오는 원래 방식으로 되돌립니다.
    @Value("${portone.api-key}")
    private String apiKey;

    @Value("${portone.api-secret}")
    private String apiSecret;

    // RestTemplate 대신 WebClient.Builder를 Bean으로 등록합니다.
    @Bean
    public WebClient.Builder webClientBuilder() {
        return WebClient.builder();
    }
}
