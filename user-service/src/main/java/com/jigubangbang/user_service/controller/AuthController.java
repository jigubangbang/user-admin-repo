package com.jigubangbang.user_service.controller;

import com.jigubangbang.user_service.model.LoginRequestDto;
import com.jigubangbang.user_service.model.LoginResponseDto;
import com.jigubangbang.user_service.model.RegisterRequestDto;
import com.jigubangbang.user_service.service.AuthService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

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
}

