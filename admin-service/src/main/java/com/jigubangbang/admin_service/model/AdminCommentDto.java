package com.jigubangbang.admin_service.model;

import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
public class AdminCommentDto {
    private int commentId;              // 댓글 ID (board_comment.id / feed_comment.id / travelmate_comment.id)
    private int originalPostId;         // 해당 댓글이 있는 콘텐츠 ID
    private String contentType;         // "community" or "feed" or "group"
    private String content;             // 댓글 내용
    private String userId;              // 작성자 ID
    private String nickname;            // 작성자 닉네임
    private String status;              // VISIBLE, BLINDED
    private LocalDateTime createdAt;    // 작성일자
}
