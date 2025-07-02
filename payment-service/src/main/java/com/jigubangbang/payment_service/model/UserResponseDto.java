package com.jigubangbang.payment_service.model;

import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * user-service로부터 사용자 정보를 받아오기 위한 DTO
 */
@Data
@NoArgsConstructor
public class UserResponseDto {
    private String userId;
    private String email;
    private String nickname;
    private String customerUid; // 우리가 필요한 빌링키!
    // ... user-service가 응답해주는 다른 필드들
}
