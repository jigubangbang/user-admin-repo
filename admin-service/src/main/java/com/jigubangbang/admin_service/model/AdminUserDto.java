package com.jigubangbang.admin_service.model;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class AdminUserDto {
    private String userId;
    private String name;
    private String nickname;
    private String email;
    private String tel;
    private String role;          // ROLE_USER, ROLE_ADMIN
    private String status;        // ACTIVE, BANNED, WITHDRAWN
    private String provider;      // kakao, naver, google
    private boolean isPremium;    // 프리미엄 여부
    private LocalDateTime createdAt;
    private int blindCount;
    private LocalDateTime bannedUntil;
}
