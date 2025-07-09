package com.jigubangbang.admin_service.model;

import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
public class AdminPostDto {
    private int postId;                 // 게시글 ID (board_post.id 또는 feed_post.id)
    private String contentType;         // "community" or "feed"
    private String title;               // 제목 (여행 피드는 제목=내용)
    private String userId;              // 작성자 ID
    private String nickname;            // 작성자 닉네임
    private String status;              // VISIBLE, BLINDED
    private LocalDateTime createdAt;    // 작성일자
}
