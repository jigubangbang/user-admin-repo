package com.jigubangbang.user_service.service;

import com.jigubangbang.user_service.mapper.UserMapper;
import com.jigubangbang.user_service.model.UpdateUserDto;
import com.jigubangbang.user_service.model.UserDto;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class UserService {

    private final UserMapper userMapper;

    // 회원 정보 조회
    public UserDto getUserInfo(String userId) {
        return userMapper.findUserById(userId);
    }

    // 회원 정보 수정 
    public void updateUserInfo(String userId, UpdateUserDto dto) {
        int updated = userMapper.updateUserInfo(userId, dto);
        if (updated == 0) {
            throw new IllegalArgumentException("회원 정보 수정에 실패했습니다.");
        }
    }
}
