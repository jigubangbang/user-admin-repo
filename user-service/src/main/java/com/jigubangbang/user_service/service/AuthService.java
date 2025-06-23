package com.jigubangbang.user_service.service;

import com.jigubangbang.user_service.mapper.UserMapper;
import com.jigubangbang.user_service.model.LoginRequestDto;
import com.jigubangbang.user_service.model.LoginResponseDto;
import com.jigubangbang.user_service.model.UserDto;
import com.jigubangbang.user_service.security.JwtTokenProvider;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final AuthenticationManager authenticationManager;
    private final UserMapper userMapper;
    private final JwtTokenProvider jwtTokenProvider;

    public LoginResponseDto login(LoginRequestDto request) {

        // 1. 사용자 인증 시도
        UsernamePasswordAuthenticationToken authenticationToken =
                new UsernamePasswordAuthenticationToken(request.getUserId(), request.getPassword());

        Authentication authentication = authenticationManager.authenticate(authenticationToken);

        // 2. 사용자 정보 조회
        UserDto user = userMapper.findUserById(request.getUserId());

        // 3. JWT 토큰 생성
        String accessToken = jwtTokenProvider.generateAccessToken(user);
        String refreshToken = jwtTokenProvider.generateRefreshToken(user.getId());

        // 4. 응답 DTO 생성 (정적 팩토리 메서드 사용)
        return LoginResponseDto.of(accessToken, refreshToken, user);
    }
}
