package com.jigubangbang.admin_service.mapper;

import com.jigubangbang.admin_service.model.AdminUserDto;
import com.jigubangbang.admin_service.model.WithdrawalDto;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface AdminUserMapper {
    List<AdminUserDto> getAllUsers();

    void updateUserStatus(@Param("userId") String userId, @Param("status") String status);

    WithdrawalDto getWithdrawalReason(@Param("userId") String userId);
}
