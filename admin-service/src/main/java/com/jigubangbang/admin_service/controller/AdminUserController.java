package com.jigubangbang.admin_service.controller;

import com.jigubangbang.admin_service.model.AdminUserDto;
import com.jigubangbang.admin_service.model.ChangeStatusDto;
import com.jigubangbang.admin_service.model.WithdrawalDto;
import com.jigubangbang.admin_service.service.AdminUserService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/admin/users")
@RequiredArgsConstructor
public class AdminUserController {

    private final AdminUserService adminUserService;

    @GetMapping
    public ResponseEntity<List<AdminUserDto>> getAllUsers() {
        List<AdminUserDto> users = adminUserService.getAllUsers();
        return ResponseEntity.ok(users);
    }

    @PutMapping("/{userId}/status")
    public ResponseEntity<Void> updateUserStatus(@PathVariable String userId, @RequestBody ChangeStatusDto dto) {
        adminUserService.updateUserStatus(userId, dto);
        return ResponseEntity.ok().build();
    }

    @GetMapping("/{userId}/withdrawal-reason")
    public ResponseEntity<WithdrawalDto> getWithdrawalReason(@PathVariable String userId) {
        WithdrawalDto reason = adminUserService.getWithdrawalReason(userId);
        return ResponseEntity.ok(reason);
    }
}
