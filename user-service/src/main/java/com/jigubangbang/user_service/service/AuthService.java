package com.jigubangbang.user_service.service;

import com.jigubangbang.user_service.exception.UserStatusException;
import com.jigubangbang.user_service.mapper.UserMapper;
import com.jigubangbang.user_service.model.AuthDto;
import com.jigubangbang.user_service.model.FindIdRequestDto;
import com.jigubangbang.user_service.model.FindIdResponseDto;
import com.jigubangbang.user_service.model.FindPwdRequestDto;
import com.jigubangbang.user_service.model.FindPwdResponseDto;
import com.jigubangbang.user_service.model.LoginRequestDto;
import com.jigubangbang.user_service.model.LoginResponseDto;
import com.jigubangbang.user_service.model.RegisterRequestDto;
import com.jigubangbang.user_service.model.SocialUserDto;
import com.jigubangbang.user_service.model.UserDto;
import com.jigubangbang.user_service.security.JwtTokenProvider;
import lombok.RequiredArgsConstructor;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.Map;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
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
    private final EmailService emailService;

    public LoginResponseDto login(LoginRequestDto request) throws UserStatusException {
        // 사용자 인증 전 상태 검사
        AuthDto authUser = userMapper.findAuthById(request.getUserId());
        if (authUser == null) {
            throw new UserStatusException("존재하지 않는 계정입니다");
        }

        // 일반 탈퇴
        if ("WITHDRAWN".equals(authUser.getStatus())) {
            if (authUser.getBlindCount() >= 5 && authUser.getBlindCount() == authUser.getLastBlindCount()) {
                throw new UserStatusException("블라인드 누적으로 탈퇴 처리된 계정입니다");
            } else {
                throw new UserStatusException("탈퇴된 계정입니다");
            }
        }

        // 정지 기간 만료 → ACTIVE 복구
        if ("BANNED".equals(authUser.getStatus())
                && authUser.getBannedUntil() != null
                && authUser.getBannedUntil().isBefore(LocalDateTime.now())) {

            userMapper.restoreUserToActive(authUser.getId());
            authUser = userMapper.findAuthById(authUser.getId());
        }

        // 블라인드 누적 탈퇴 (5회 이상, 이전 처리 이력보다 많을 때)
        if ("ACTIVE".equals(authUser.getStatus())
                && authUser.getBlindCount() >= 5
                && authUser.getBlindCount() > authUser.getLastBlindCount()) {

            userMapper.updateUserAsWithdrawn(authUser.getId());
            userMapper.updateLastBlindCount(authUser.getId(), authUser.getBlindCount());
            throw new UserStatusException("블라인드 누적으로 탈퇴 처리된 계정입니다");
        }

        // 블라인드 누적 정지 (3회 이상, 이전 처리 이력보다 많을 때)
        if ((authUser.getStatus().equals("ACTIVE") || authUser.getStatus().equals("BANNED"))
                && authUser.getBlindCount() >= 3
                && authUser.getBlindCount() > authUser.getLastBlindCount()) {

            LocalDateTime until = LocalDateTime.now().plusDays(7);
            userMapper.updateStatusAndBannedUntil(authUser.getId(), "BANNED", until);
            userMapper.updateLastBlindCount(authUser.getId(), authUser.getBlindCount());

            long daysLeft = ChronoUnit.DAYS.between(LocalDateTime.now(), until);
            throw new UserStatusException("블라인드 누적으로 정지된 계정입니다\n정지 해제: "
                    + until.toLocalDate() + " (D-" + daysLeft + ")");
        }

        // 일반 정지
        if ("BANNED".equals(authUser.getStatus())) {
            if (authUser.getBannedUntil() == null) {
                throw new UserStatusException("정지된 계정입니다");
            } else {
                long daysLeft = ChronoUnit.DAYS.between(LocalDateTime.now(), authUser.getBannedUntil());
                throw new UserStatusException("블라인드 누적으로 정지된 계정입니다\n정지 해제: "
                        + authUser.getBannedUntil().toLocalDate() + " (D-" + daysLeft + ")");
            }
        }

        // Spring Security 인증 처리
        UsernamePasswordAuthenticationToken authenticationToken = new UsernamePasswordAuthenticationToken(
                request.getUserId(), request.getPassword());

        try {
            Authentication authentication = authenticationManager.authenticate(authenticationToken);
            SecurityContextHolder.getContext().setAuthentication(authentication);
        } catch (Exception e) {
            throw new IllegalArgumentException("아이디 또는 비밀번호가 일치하지 않습니다");
        }

        // 사용자 정보 조회 및 JWT 토큰 생성
        UserDto user = userMapper.findUserById(request.getUserId());
        String accessToken = jwtTokenProvider.generateAccessToken(user);
        String refreshToken = jwtTokenProvider.generateRefreshToken(user.getId());

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

    public LoginResponseDto socialLogin(String code, String provider) throws UserStatusException {
        // 1. 소셜 플랫폼에서 사용자 정보 조회
        SocialUserDto socialUser = socialOAuthService.getUserInfo(code, provider);

        // 2. 기존 사용자 조회 (이메일 기준)
        UserDto existingUser = userMapper.findByEmail(socialUser.getEmail());

        // 3. 신규 사용자 등록
        if (existingUser == null) {
            if (userMapper.existsByEmail(socialUser.getEmail())) {
                existingUser = userMapper.findByEmail(socialUser.getEmail());
            } else {
                try {
                    String userId = generateRandomUserId();

                    RegisterRequestDto newUser = new RegisterRequestDto();
                    newUser.setUserId(userId);
                    newUser.setPassword(""); // 소셜 로그인은 비밀번호 없음
                    newUser.setConfirmPassword("");
                    newUser.setName(socialUser.getName());
                    newUser.setNickname(socialUser.getNickname());
                    newUser.setEmail(socialUser.getEmail());
                    newUser.setTel(socialUser.getTel());
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

        // 4. 사용자 상태 검사 (AuthDto 기반)
        AuthDto authUser = userMapper.findAuthByEmail(socialUser.getEmail());
        if (authUser == null) {
            throw new UserStatusException("존재하지 않는 계정입니다");
        }

        if ("WITHDRAWN".equals(authUser.getStatus())) {
            if (authUser.getBlindCount() >= 5 && authUser.getBlindCount() == authUser.getLastBlindCount()) {
                throw new UserStatusException("블라인드 누적으로 탈퇴 처리된 계정입니다");
            } else {
                throw new UserStatusException("탈퇴된 계정입니다");
            }
        }

        if ("BANNED".equals(authUser.getStatus())
                && authUser.getBannedUntil() != null
                && authUser.getBannedUntil().isBefore(LocalDateTime.now())) {
            userMapper.restoreUserToActive(authUser.getId());
            authUser = userMapper.findAuthById(authUser.getId());
        }

        if ("ACTIVE".equals(authUser.getStatus())
                && authUser.getBlindCount() >= 5
                && authUser.getBlindCount() > authUser.getLastBlindCount()) {
            userMapper.updateUserAsWithdrawn(authUser.getId());
            userMapper.updateLastBlindCount(authUser.getId(), authUser.getBlindCount());
            throw new UserStatusException("블라인드 누적으로 탈퇴 처리된 계정입니다");
        }

        if ((authUser.getStatus().equals("ACTIVE") || authUser.getStatus().equals("BANNED"))
                && authUser.getBlindCount() >= 3
                && authUser.getBlindCount() > authUser.getLastBlindCount()) {
            LocalDateTime until = LocalDateTime.now().plusDays(7);
            userMapper.updateStatusAndBannedUntil(authUser.getId(), "BANNED", until);
            userMapper.updateLastBlindCount(authUser.getId(), authUser.getBlindCount());

            long daysLeft = ChronoUnit.DAYS.between(LocalDateTime.now(), until);
            throw new UserStatusException("블라인드 누적으로 정지된 계정입니다\n정지 해제: "
                    + until.toLocalDate() + " (D-" + daysLeft + ")");
        }

        if ("BANNED".equals(authUser.getStatus())) {
            if (authUser.getBannedUntil() == null) {
                throw new UserStatusException("정지된 계정입니다");
            } else {
                long daysLeft = ChronoUnit.DAYS.between(LocalDateTime.now(), authUser.getBannedUntil());
                throw new UserStatusException("블라인드 누적으로 정지된 계정입니다\n정지 해제: "
                        + authUser.getBannedUntil().toLocalDate() + " (D-" + daysLeft + ")");
            }
        }

        // 5. 사용자 정보 조회 및 JWT 발급
        UserDto user = userMapper.findUserById(authUser.getId());
        String accessToken = jwtTokenProvider.generateAccessToken(user);
        String refreshToken = jwtTokenProvider.generateRefreshToken(user.getId());

        return LoginResponseDto.of(accessToken, refreshToken, user);
    }

    private String generateRandomUserId() {
        return "user" + java.util.UUID.randomUUID().toString().replace("-", "").substring(0, 8);
    }

    public LoginResponseDto refreshAccessToken(String tokenHeader) {
        // 순수 토큰만 추출
        String token = tokenHeader.replace("Bearer ", "");

        // 유효성 검사: refresh token인지 확인
        if (!jwtTokenProvider.validateToken(token) ||
                !"refresh".equals(jwtTokenProvider.getTokenType(token))) {
            throw new IllegalArgumentException("유효하지 않은 RefreshToken입니다.");
        }

        // 토큰에서 사용자 ID 추출
        String userId = jwtTokenProvider.getUserIdFromToken(token);

        // 사용자 정보 조회
        UserDto user = userMapper.findUserById(userId);
        if (user == null) {
            throw new IllegalArgumentException("사용자 정보를 찾을 수 없습니다.");
        }

        // 새 AccessToken 발급 (RefreshToken은 그대로 재사용)
        String newAccessToken = jwtTokenProvider.generateAccessToken(user);

        return LoginResponseDto.of(newAccessToken, token, user); // 기존 RefreshToken 그대로
    }

    public ResponseEntity<?> findUserId(FindIdRequestDto dto) {
        FindIdResponseDto result = userMapper.findByNameAndEmail(dto.getName(), dto.getEmail());

        if (result == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
        }

        if (result.getProvider() != null) {
            return ResponseEntity.ok(Map.of("isSocial", true));
        }
        emailService.sendFoundUserId(dto.getEmail(), result.getUserId());
        return ResponseEntity.ok(Map.of("userId", result.getUserId()));
    }

    public ResponseEntity<?> findUserPassword(FindPwdRequestDto dto) {
        FindPwdResponseDto result = userMapper.findByUserIdNameEmail(
                dto.getUserId(), dto.getName(), dto.getEmail());

        if (result == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
        }

        if (result.getProvider() != null) {
            return ResponseEntity.ok(Map.of("isSocial", true));
        }

        if (result.getTempPwdAt() != null &&
                result.getTempPwdAt().isAfter(java.time.LocalDateTime.now().minusMinutes(30))) {
            result.setLimited(true);
            return ResponseEntity.ok(result);
        }

        // 임시 비밀번호 생성
        String tempPassword = generateTempPassword();
        String encodedPassword = passwordEncoder.encode(tempPassword);

        // 비밀번호 및 발급시각 갱신
        userMapper.updatePasswordAndTempPwdAt(result.getUserId(), encodedPassword, java.time.LocalDateTime.now());

        // 이메일 발송
        emailService.sendTempPassword(dto.getEmail(), tempPassword);

        return ResponseEntity.ok(Map.of("issued", true));
    }

    private String generateTempPassword() {
        return java.util.UUID.randomUUID().toString().replace("-", "").substring(0, 10);
    }
}