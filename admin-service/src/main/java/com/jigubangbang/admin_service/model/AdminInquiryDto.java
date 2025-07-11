package com.jigubangbang.admin_service.model;

import java.time.LocalDateTime;
import java.util.List;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class AdminInquiryDto {
    private int id;                     // 문의 ID
    private String userId;              // 작성자 ID
    private String nickname;            // 작성자 닉네임
    private String category;            // 문의 카테고리
    private String title;               // 문의 제목
    private String content;             // 문의 내용
    private String attachment;          // 첨부파일
    private List<AttachmentInfo> attachments;
    private String status;              // 문의 상태 (PENDING, REPLIED)
    private LocalDateTime createdAt;    // 작성 일시
    private String adminReply;          // 관리자 답변
    private String adminId;             // 답변한 관리자 ID 
    private LocalDateTime repliedAt;    // 답변 일시

    @Getter
    @Setter
    public static class AttachmentInfo {
        private String originalName;
        private String url;
    }
}
