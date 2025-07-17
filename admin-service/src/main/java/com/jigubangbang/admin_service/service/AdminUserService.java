package com.jigubangbang.admin_service.service;

import com.jigubangbang.admin_service.mapper.AdminUserMapper;
import com.jigubangbang.admin_service.model.AdminUserDto;
import com.jigubangbang.admin_service.model.ChangeStatusDto;
import com.jigubangbang.admin_service.model.WithdrawalDto;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class AdminUserService {

    private final AdminUserMapper adminUserMapper;

    public List<AdminUserDto> getAllUsers() {
        return adminUserMapper.getAllUsers();
    }

    public void updateUserStatus(String userId, ChangeStatusDto dto) {
        adminUserMapper.updateUserStatus(userId, dto.getStatus());
    }

    public WithdrawalDto getWithdrawalReason(String userId) {
        return adminUserMapper.getWithdrawalReason(userId);
    }
}
