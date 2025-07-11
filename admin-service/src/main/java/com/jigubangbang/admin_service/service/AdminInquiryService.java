package com.jigubangbang.admin_service.service;

import com.jigubangbang.admin_service.chat_service.NotificationServiceClient;
import com.jigubangbang.admin_service.mapper.AdminInquiryMapper;
import com.jigubangbang.admin_service.model.AdminInquiryDto;
import com.jigubangbang.admin_service.model.chat_service.InquiryNotificationRequestDto;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

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

        notificationServiceClient.createInquiryAnsweredNotification(notification);
    }

}
