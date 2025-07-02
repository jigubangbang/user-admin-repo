package com.jigubangbang.user_service.service;

import com.jigubangbang.user_service.mapper.UserMapper;
import com.jigubangbang.user_service.model.UpdateUserDto;
import com.jigubangbang.user_service.model.UserDto;

import lombok.RequiredArgsConstructor;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;

import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class UserService {

    private final UserMapper userMapper;

    // 회원 정보 조회
    public UserDto getUserInfo(String userId) {
        UserDto dto = userMapper.findUserById(userId);

        // 닉네임 다음 변경 가능일 계산
        LocalDateTime lastChanged = userMapper.getNicknameUpdatedAt(userId);
        if (lastChanged != null) {
            LocalDateTime unlockAt = lastChanged.plusDays(30);
            dto.setNicknameUnlockAt(unlockAt);
        }

        return dto;
    }

    // 회원 정보 수정
    public void updateUserInfo(String userId, UpdateUserDto dto) {
        String currentNickname = userMapper.findUserById(userId).getNickname();

        if (dto.getNickname() != null) {
            // 닉네임이 변경된 경우에만 30일 제한 검사
            if (!dto.getNickname().equals(currentNickname)) {
                LocalDateTime lastUpdated = userMapper.getNicknameUpdatedAt(userId);
                if (lastUpdated != null) {
                    long daysSince = ChronoUnit.DAYS.between(lastUpdated, LocalDateTime.now());
                    if (daysSince < 30) {
                        throw new IllegalArgumentException("닉네임은 변경 후 30일이 지나야 변경 가능합니다.");
                    }
                }
            } else {
                // 기존 닉네임과 동일하면 nickname_updated_at 갱신 X
                dto.setNickname(null);
            }
        }

        int updated = userMapper.updateUserInfo(userId, dto);
        if (updated == 0) {
            throw new IllegalArgumentException("회원 정보 수정에 실패했습니다.");
        }
    }
}
