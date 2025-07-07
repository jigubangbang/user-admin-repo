package com.jigubangbang.user_service.controller;

import com.jigubangbang.user_service.model.AuthDto;
import com.jigubangbang.user_service.model.ChangeEmailDto;
import com.jigubangbang.user_service.model.ChangePwdDto;
import com.jigubangbang.user_service.model.UpdateUserDto;
import com.jigubangbang.user_service.model.UserDto;
import com.jigubangbang.user_service.model.WithdrawalRequestDto;
import com.jigubangbang.user_service.service.UserService;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/user")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;

    // 내 정보 조회
    @GetMapping("/me")
    public ResponseEntity<UserDto> getMyInfo(@AuthenticationPrincipal AuthDto user) {
        UserDto userInfo = userService.getUserInfo(user.getUsername());
        return ResponseEntity.ok(userInfo);
    }

    // 내 정보 수정
    @PutMapping("/me")
    public ResponseEntity<String> updateMyInfo(@AuthenticationPrincipal AuthDto user,
            @Valid @RequestBody UpdateUserDto dto) {
        try {
            userService.updateUserInfo(user.getUsername(), dto);
            return ResponseEntity.ok("회원정보가 성공적으로 수정되었습니다.");
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    // 비밀번호 변경
    @PutMapping("/password")
    public ResponseEntity<String> changePassword(@AuthenticationPrincipal AuthDto user,
            @Valid @RequestBody ChangePwdDto dto) {
        try {
            userService.changePassword(user.getUsername(), dto);
            return ResponseEntity.ok("비밀번호가 성공적으로 변경되었습니다.");
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    // 이메일 변경 요청
    @PostMapping("/email/change-request")
    public ResponseEntity<String> requestEmailChange(@AuthenticationPrincipal AuthDto user,
            @RequestParam("email") @Email String newEmail) {
        try {
            userService.requestEmailChange(user.getUsername(), newEmail);
            return ResponseEntity.ok("인증 코드가 이메일로 전송되었습니다.");
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    // 이메일 변경 확인
    @PutMapping("/email/change-confirm")
    public ResponseEntity<String> confirmEmailChange(@AuthenticationPrincipal AuthDto user,
            @Valid @RequestBody ChangeEmailDto dto) {
        try {
            userService.confirmEmailChange(user.getUsername(), dto);
            return ResponseEntity.ok("이메일이 성공적으로 변경되었습니다.");
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    // 자발적 회원 탈퇴
    @DeleteMapping("/me")
    public ResponseEntity<?> withdrawUser(@AuthenticationPrincipal AuthDto user,
            @Valid @RequestBody WithdrawalRequestDto dto) {
        try {
            userService.withdrawUser(user.getUsername(), dto);
            return ResponseEntity.noContent().build(); // 탈퇴 성공: 204 No Content
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(e.getMessage()); // 예외 시: 400 + 메시지
        }
    }
    
}
