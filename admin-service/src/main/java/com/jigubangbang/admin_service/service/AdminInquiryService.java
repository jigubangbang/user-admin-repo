package com.jigubangbang.admin_service.service;

import com.jigubangbang.admin_service.chat_service.NotificationServiceClient;
import com.jigubangbang.admin_service.mapper.AdminInquiryMapper;
import com.jigubangbang.admin_service.model.AdminInquiryDto;
import com.jigubangbang.admin_service.model.chat_service.InquiryNotificationRequestDto;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
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
        AdminInquiryDto inquiry = adminInquiryMapper.getInquiryById(inquiryId);

        // 이 부분 추가
        if (inquiry.getAttachment() != null && !inquiry.getAttachment().isBlank()) {
            inquiry.setAttachments(parseJsonToAttachments(inquiry.getAttachment()));
        }

        return inquiry;
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

    // JSON 문자열을 AttachmentInfo 리스트로 파싱
    private List<AdminInquiryDto.AttachmentInfo> parseJsonToAttachments(String jsonString) {
        List<AdminInquiryDto.AttachmentInfo> attachments = new ArrayList<>();
        if (jsonString == null || jsonString.trim().isEmpty()) {
            return attachments;
        }

        try {
            String content = jsonString.trim();
            if (content.startsWith("[") && content.endsWith("]")) {
                content = content.substring(1, content.length() - 1);
            }

            if (content.trim().isEmpty()) {
                return attachments;
            }

            String[] objects = content.split("\\},\\{");
            for (int i = 0; i < objects.length; i++) {
                String obj = objects[i];

                if (i == 0 && obj.startsWith("{")) {
                    obj = obj.substring(1);
                }
                if (i == objects.length - 1 && obj.endsWith("}")) {
                    obj = obj.substring(0, obj.length() - 1);
                }

                AdminInquiryDto.AttachmentInfo info = new AdminInquiryDto.AttachmentInfo();

                // originalName 추출
                int nameStart = obj.indexOf("\"originalName\":\"") + 16;
                int nameEnd = obj.indexOf("\",\"url\":");
                if (nameStart > 15 && nameEnd > nameStart) {
                    String originalName = obj.substring(nameStart, nameEnd);
                    info.setOriginalName(originalName);
                }

                // url 추출
                int urlStart = obj.indexOf("\"url\":\"") + 7;
                int urlEnd = obj.lastIndexOf("\"");
                if (urlStart > 6 && urlEnd > urlStart) {
                    String url = obj.substring(urlStart, urlEnd);
                    info.setUrl(url);
                }

                attachments.add(info);
            }
        } catch (Exception e) {
            System.err.println("JSON 파싱 오류: " + e.getMessage());
            e.printStackTrace();
        }

        return attachments;
    }

}
