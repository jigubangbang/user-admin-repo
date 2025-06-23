package com.jigubangbang.user_service.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
@AllArgsConstructor
public class LoginResponseDto {
    private String accessToken;
    private String refreshToken;
    private UserDto user;

    public static LoginResponseDto of(String accessToken, String refreshToken, UserDto user) {
        return LoginResponseDto.builder()
                .accessToken(accessToken)
                .refreshToken(refreshToken)
                .user(user)
                .build();
    }
}

