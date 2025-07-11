package com.jigubangbang.admin_service.service;

import com.jigubangbang.admin_service.chat_service.NotificationServiceClient;
import com.jigubangbang.admin_service.mapper.AdminInquiryMapper;
import com.jigubangbang.admin_service.model.AdminInquiryDto;
import com.jigubangbang.admin_service.model.chat_service.InquiryNotificationRequestDto;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.springframework.stereotype.Service;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class AdminInquiryService {

    private final AdminInquiryMapper adminInquiryMapper;
    private final NotificationServiceClient notificationServiceClient;

    // 전체 조회
    public List<AdminInquiryDto> getInquiryList() {
        return adminInquiryMapper.getAllInquiries();
    }

    // 상세 조회
    public AdminInquiryDto getInquiryDetail(int inquiryId) {
        return adminInquiryMapper.getInquiryById(inquiryId);
    }

    // 답변 등록
    public void replyToInquiry(int inquiryId, String adminId, String reply) {
        int updated = adminInquiryMapper.updateInquiryReply(inquiryId, adminId, reply);
        if (updated == 0) {
            throw new IllegalArgumentException("문의 답변 등록에 실패했습니다.");
        }

        AdminInquiryDto inquiry = adminInquiryMapper.getInquiryById(inquiryId);

        InquiryNotificationRequestDto notification = InquiryNotificationRequestDto.builder()
                .userId(inquiry.getUserId())
                .message("문의하신 내용에 대한 답변이 등록되었습니다.")
                .relatedUrl("/user/inquiry/" + inquiryId)
                .senderId(adminId)
                .build();

        log.info("[AdminService] 알림 요청 전송: userId={}, message={}, url={}, senderId={}",
                notification.getUserId(),
                notification.getMessage(),
                notification.getRelatedUrl(),
                notification.getSenderId());
        log.info("[AdminService] 문의 ID={}, 사용자 ID={}, 관리자 ID={}", inquiryId, inquiry.getUserId(), adminId);
        

        try {
            notificationServiceClient.createInquiryAnsweredNotification(notification);
            log.info("[AdminService] 알림 요청 완료");
        } catch (Exception e) {
            log.error("[AdminService] 알림 요청 실패: {}", e.getMessage());
        }

        // notificationServiceClient.createInquiryAnsweredNotification(notification);
    }

}
