package com.jigubangbang.admin_service.chat_service;

import java.util.Map;

import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.bind.annotation.RequestBody;

import com.jigubangbang.admin_service.model.chat_service.InquiryNotificationRequestDto;

import lombok.extern.slf4j.Slf4j;

@Component
@Slf4j
public class NotificationServiceClientFallback implements NotificationServiceClient {
    
    @Override
     public ResponseEntity<Map<String, Object>> createInquiryAnsweredNotification(@RequestBody InquiryNotificationRequestDto request) {
        log.warn("[Fallback] 일대일 답변 알림 전송 실패 - 메인 기능은 정상 처리됨. 요청 데이터: userId={}, message={}, url={}, senderId={}",
            request.getUserId(), request.getMessage(), request.getRelatedUrl(), request.getSenderId());

        return ResponseEntity.ok(Map.of("success", false, "fallback", true));
    }

}
