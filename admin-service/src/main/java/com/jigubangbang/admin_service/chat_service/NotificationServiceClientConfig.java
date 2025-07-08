package com.jigubangbang.admin_service.chat_service;

import com.jigubangbang.admin_service.security.FeignClientInterceptor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class NotificationServiceClientConfig {

    @Bean
    public FeignClientInterceptor notificationFeignClientInterceptor() {
        return new FeignClientInterceptor();
    }

}
