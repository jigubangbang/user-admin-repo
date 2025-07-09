package com.jigubangbang.admin_service.model;

import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
public class AdminGroupDto {
    private int groupId;                   // 그룹 ID (travelmate_post.id / travel_info.id)
    private String contentType;            // "mate" or "info"
    private String title;                  // 제목
    private String simpleDescription;      // 요약 설명
    private String userId;                 // 작성자 ID
    private String nickname;               // 작성자 닉네임
    private String status;                 // VISIBLE, BLINDED
    private LocalDateTime createdAt;       // 작성일시
}
