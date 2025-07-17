package com.jigubangbang.user_service.model;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class SocialRequestDto {
    private String code;      // 소셜 플랫폼에서 받은 인증 코드
    private String provider;  // "kakao", "naver", "google"
}