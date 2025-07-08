package com.jigubangbang.admin_service.chat_service;

import java.util.Map;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

import com.jigubangbang.admin_service.model.chat_service.BadgeNotificationRequestDto;
import com.jigubangbang.admin_service.model.chat_service.InquiryNotificationRequestDto;

@FeignClient( name="chat-service", configuration = NotificationServiceClientConfig.class, fallback = NotificationServiceClientFallback.class)
public interface NotificationServiceClient {

    // 뱃지 수거 알림 (관리자가 점검 후 뱃지 취소)
    @PostMapping("/admin/badges/revoked")
    public ResponseEntity<Map<String, Object>> createBadgeRevokedNotification(@RequestBody BadgeNotificationRequestDto request);
    
    // 1:1 문의 답변/처리 알림
    @PostMapping("/inqury")
    public ResponseEntity<Map<String, Object>> createInquiryAnsweredNotification(@RequestBody InquiryNotificationRequestDto request);
}
