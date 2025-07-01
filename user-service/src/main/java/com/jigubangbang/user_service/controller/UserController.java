package com.jigubangbang.user_service.controller;

import com.jigubangbang.user_service.model.AuthDto;
import com.jigubangbang.user_service.model.UpdateUserDto;
import com.jigubangbang.user_service.model.UserDto;
import com.jigubangbang.user_service.service.UserService;

import jakarta.validation.Valid;
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
    public ResponseEntity<String> updateMyInfo(@AuthenticationPrincipal AuthDto user, @Valid @RequestBody UpdateUserDto dto) {
        userService.updateUserInfo(user.getUsername(), dto);
        return ResponseEntity.ok("회원정보가 성공적으로 수정되었습니다.");
    }
}
