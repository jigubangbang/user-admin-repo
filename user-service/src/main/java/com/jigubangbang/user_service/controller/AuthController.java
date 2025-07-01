package com.jigubangbang.user_service.controller;

import com.jigubangbang.user_service.model.EmailDto;
import com.jigubangbang.user_service.model.FindIdRequestDto;
import com.jigubangbang.user_service.model.FindPwdRequestDto;
import com.jigubangbang.user_service.model.LoginRequestDto;
import com.jigubangbang.user_service.model.LoginResponseDto;
import com.jigubangbang.user_service.model.RegisterRequestDto;
import com.jigubangbang.user_service.model.SocialRequestDto;
import com.jigubangbang.user_service.service.AuthService;
import com.jigubangbang.user_service.service.EmailService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;
    private final EmailService emailService;

    @PostMapping("/login")
    public ResponseEntity<LoginResponseDto> login(@RequestBody LoginRequestDto request) {
        LoginResponseDto response = authService.login(request);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/register")
    public ResponseEntity<String> register(@RequestBody @Valid RegisterRequestDto dto) {
        authService.register(dto);
        return ResponseEntity.ok("회원가입이 완료되었습니다.");
    }

    @GetMapping("/check-id/{userId}")
    public ResponseEntity<Boolean> checkUserId(@PathVariable String userId) {
        boolean isDuplicate = authService.isUserIdDuplicate(userId);
        return ResponseEntity.ok(isDuplicate);
    }

    @GetMapping("/check-email/{email}")
    public ResponseEntity<Boolean> checkEmail(@PathVariable String email) {
        boolean isDuplicate = authService.isEmailDuplicate(email);
        return ResponseEntity.ok(isDuplicate);
    }

    @PostMapping("/email/send")
    public ResponseEntity<String> sendEmailCode(@RequestBody EmailDto request) {
        emailService.sendVerificationCode(request.getEmail());
        return ResponseEntity.ok("인증 코드가 이메일로 전송되었습니다.");
    }

    @PostMapping("/email/verify")
    public ResponseEntity<String> verifyEmailCode(@RequestBody EmailDto request) {
        boolean verified = emailService.verifyCode(request.getEmail(), request.getCode());
        if (verified) {
            return ResponseEntity.ok("이메일 인증에 성공했습니다.");
        } else {
            return ResponseEntity.badRequest().body("유효하지 않거나 만료된 인증코드입니다.");
        }
    }
    
    @PostMapping("/{provider}")
    public ResponseEntity<LoginResponseDto> socialLogin(
            @PathVariable String provider,
            @RequestBody SocialRequestDto request) {
        LoginResponseDto response = authService.socialLogin(request.getCode(), provider);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/refresh-token")
    public ResponseEntity<LoginResponseDto> refreshAccessToken(
            @RequestHeader("Authorization") String refreshTokenHeader) {
        try {
            LoginResponseDto response = authService.refreshAccessToken(refreshTokenHeader);
            return ResponseEntity.ok(response);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(401).build(); // RefreshToken 유효하지 않음
        }
    }

    @PostMapping("/find-id")
    public ResponseEntity<?> findUserId(@RequestBody FindIdRequestDto dto) {
        return authService.findUserId(dto);
    }

    @PostMapping("/find-password")
    public ResponseEntity<?> findUserPassword(@RequestBody FindPwdRequestDto dto) {
        return authService.findUserPassword(dto);
    }

}
