package com.jigubangbang.user_service.model;

import java.time.LocalDateTime;
import java.util.List;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class InquiryDto {
    private int id;
    private String userId;
    private String title;
    private String content;
    private String category;            // ACC, PAY, SVC, REPORT, SUGGEST, ETC
    private String status;              // PENDING, REPLIED
    private String adminReply;
    private LocalDateTime createdAt;
    private LocalDateTime repliedAt;

    private List<AttachmentInfo> attachments;
    private String attachment;          // DB 저장용 JSON 문자열

    @Getter
    @Setter
    public static class AttachmentInfo {
        private String originalName;
        private String url;
    }
}