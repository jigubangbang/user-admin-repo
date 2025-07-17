package com.jigubangbang.user_service.model;

import lombok.Builder;
import lombok.Data;

/**
 * 다른 서비스에 사용자 정보를 전달하기 위한 DTO
 */
@Data
@Builder
public class UserResponseDto {
    private String userId;
    private String email;
    private String nickname;
    private String customerUid; // payment-service가 필요로 하는 빌링키
    private boolean isPremium; // 사용자의 프리미엄 상태
}
