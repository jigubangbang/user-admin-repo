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
    private List<String> attachments;   // S3 URL 리스트
    private String attachment;          // DB 저장용 문자열
    private String status;              // PENDING, REPLIED
    private String adminReply;
    private LocalDateTime createdAt;
    private LocalDateTime repliedAt;
}