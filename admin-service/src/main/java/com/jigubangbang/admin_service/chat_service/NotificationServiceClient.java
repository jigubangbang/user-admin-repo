package com.jigubangbang.admin_service.chat_service;

import java.util.Map;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

import com.jigubangbang.admin_service.model.chat_service.InquiryNotificationRequestDto;

@FeignClient( name="chat-service", configuration = NotificationServiceClientConfig.class, fallback = NotificationServiceClientFallback.class)
public interface NotificationServiceClient { 
    // 1:1 문의 답변/처리 알림
    @PostMapping("/inqury")
    public ResponseEntity<Map<String, Object>> createInquiryAnsweredNotification(@RequestBody InquiryNotificationRequestDto request);
}
