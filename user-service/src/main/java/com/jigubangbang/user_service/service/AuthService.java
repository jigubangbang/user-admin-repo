package com.jigubangbang.user_service.service;

import com.jigubangbang.user_service.mapper.UserMapper;
import com.jigubangbang.user_service.model.LoginRequestDto;
import com.jigubangbang.user_service.model.LoginResponseDto;
import com.jigubangbang.user_service.model.RegisterRequestDto;
import com.jigubangbang.user_service.model.SocialUserDto;
import com.jigubangbang.user_service.model.UserDto;
import com.jigubangbang.user_service.security.JwtTokenProvider;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final AuthenticationManager authenticationManager;
    private final UserMapper userMapper;
    private final JwtTokenProvider jwtTokenProvider;
    private final PasswordEncoder passwordEncoder;
    private final SocialOAuthService socialOAuthService;

    public LoginResponseDto login(LoginRequestDto request) {

        // 1. 사용자 인증 시도
        UsernamePasswordAuthenticationToken authenticationToken =
                new UsernamePasswordAuthenticationToken(request.getUserId(), request.getPassword());

        Authentication authentication = authenticationManager.authenticate(authenticationToken);
        SecurityContextHolder.getContext().setAuthentication(authentication);

        // 2. 사용자 정보 조회
        UserDto user = userMapper.findUserById(request.getUserId());

        // 3. JWT 토큰 생성
        String accessToken = jwtTokenProvider.generateAccessToken(user);
        String refreshToken = jwtTokenProvider.generateRefreshToken(user.getId());

        // 4. 응답 DTO 생성 (정적 팩토리 메서드 사용)
        return LoginResponseDto.of(accessToken, refreshToken, user);
    }

    public void register(RegisterRequestDto dto) {
        // 아이디 중복 검사
        if (userMapper.existsByUserId(dto.getUserId())) {
            throw new IllegalArgumentException("이미 사용 중인 아이디입니다.");
        }
        // 이메일 중복 검사
        if (userMapper.existsByEmail(dto.getEmail())) {
            throw new IllegalArgumentException("이미 사용 중인 이메일입니다.");
        }
        // 비밀번호 일치 확인
        if (!dto.getPassword().equals(dto.getConfirmPassword())) {
            throw new IllegalArgumentException("비밀번호가 일치하지 않습니다.");
        }
        // 비밀번호 암호화 및 저장
        dto.setPassword(passwordEncoder.encode(dto.getPassword()));
        userMapper.insertUser(dto);
    }

    public boolean isUserIdDuplicate(String userId) {
        return userMapper.existsByUserId(userId);
    }

    public boolean isEmailDuplicate(String email) {
        return userMapper.existsByEmail(email);
    }

    public LoginResponseDto socialLogin(String code, String provider) {
    // 1. 소셜 플랫폼에서 사용자 정보 조회
    SocialUserDto socialUser = socialOAuthService.getUserInfo(code, provider);

    // 2. 기존 사용자 조회 (이메일 기준)
    UserDto existingUser = userMapper.findByEmail(socialUser.getEmail());

        if (existingUser == null) {
            if (userMapper.existsByEmail(socialUser.getEmail())) {
                existingUser = userMapper.findByEmail(socialUser.getEmail());
            } else {
                try {
                    // 3. 신규 사용자 등록
                    String userId = generateRandomUserId();

                    RegisterRequestDto newUser = new RegisterRequestDto();
                    newUser.setUserId(userId);
                    newUser.setPassword(""); // 소셜 로그인은 비밀번호 없음
                    newUser.setConfirmPassword("");
                    newUser.setName("");
                    newUser.setNickname(socialUser.getNickname());
                    newUser.setEmail(socialUser.getEmail());
                    newUser.setTel(null);
                    newUser.setAgreedRequired(true);
                    newUser.setAgreedOptional(false);
                    newUser.setProvider(socialUser.getProvider()); 
                    newUser.setProviderId(socialUser.getProviderId());

                    userMapper.insertUser(newUser);
                    existingUser = userMapper.findByEmail(socialUser.getEmail());
                } catch (Exception e) {
                    existingUser = userMapper.findByEmail(socialUser.getEmail());
                }
            }
        }

        // 4. JWT 토큰 발급
        String accessToken = jwtTokenProvider.generateAccessToken(existingUser);
        String refreshToken = jwtTokenProvider.generateRefreshToken(existingUser.getId());

        return LoginResponseDto.of(accessToken, refreshToken, existingUser);
    }


    private String generateRandomUserId() {
        return "user" + java.util.UUID.randomUUID().toString().replace("-", "").substring(0, 8);
    }
}