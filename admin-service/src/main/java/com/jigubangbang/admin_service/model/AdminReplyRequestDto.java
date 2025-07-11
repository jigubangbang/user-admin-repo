package com.jigubangbang.admin_service.model;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class AdminReplyRequestDto {
    private String reply;       // 관리자 답변 내용
    private String adminId;     // 관리자 ID
}
