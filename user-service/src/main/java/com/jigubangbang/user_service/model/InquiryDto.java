package com.jigubangbang.user_service.model;

import java.time.LocalDateTime;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class InquiryDto {
    private int id;
    private String userId;
    private String title;
    private String content;
    private String category;        // ACC, PAY, SVC, REPORT, SUGGEST, ETC
    private String attachment;
    private String status;          // PENDING, REPLIED
    private String adminReply;
    private LocalDateTime createdAt;
    private LocalDateTime repliedAt;
}