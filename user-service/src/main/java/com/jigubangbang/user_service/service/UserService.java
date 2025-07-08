package com.jigubangbang.user_service.service;

import com.jigubangbang.user_service.mapper.UserMapper;
import com.jigubangbang.user_service.model.ChangeEmailDto;
import com.jigubangbang.user_service.model.ChangePwdDto;
import com.jigubangbang.user_service.model.UpdateUserDto;
import com.jigubangbang.user_service.model.UserDto;
import com.jigubangbang.user_service.model.WithdrawalRequestDto;

import lombok.RequiredArgsConstructor;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class UserService {

    private final UserMapper userMapper;
    private final PasswordEncoder passwordEncoder;
    private final EmailService emailService;

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
                        throw new IllegalArgumentException("닉네임은 30일마다 변경 가능합니다.");
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

    // 비밀번호 변경
    public void changePassword(String userId, ChangePwdDto dto) {
        String currentPasswordHash = userMapper.getCurrentPassword(userId);

        if (!passwordEncoder.matches(dto.getCurrentPassword(), currentPasswordHash)) {
            throw new IllegalArgumentException("현재 비밀번호가 일치하지 않습니다.");
        }

        String encodedNewPassword = passwordEncoder.encode(dto.getNewPassword());
        int updated = userMapper.updatePassword(userId, encodedNewPassword);

        if (updated == 0) {
            throw new IllegalArgumentException("비밀번호 변경에 실패했습니다.");
        }
    }

    // 이메일 변경 요청
    public void requestEmailChange(String userId, String newEmail) {
        String currentEmail = userMapper.findUserById(userId).getEmail();

        if (currentEmail.equals(newEmail)) {
            throw new IllegalArgumentException("기존 이메일과 동일합니다.");
        }

        if (userMapper.existsByEmail(newEmail)) {
            throw new IllegalArgumentException("이미 사용 중인 이메일입니다.");
        }

        emailService.sendVerificationCode(newEmail);
    }

    // 이메일 변경 확인
    public void confirmEmailChange(String userId, ChangeEmailDto dto) {
        boolean verified = emailService.verifyCode(dto.getEmail(), dto.getEmailCode());

        if (!verified) {
            throw new IllegalArgumentException("인증코드가 올바르지 않거나 만료되었습니다. 다시 시도해주세요.");
        }

        int updated = userMapper.updateEmail(userId, dto.getEmail());
        if (updated == 0) {
            throw new IllegalArgumentException("이메일 변경에 실패했습니다.");
        }
    }

    // 자발적 회원 탈퇴
    @Transactional
    public void withdrawUser(String userId, WithdrawalRequestDto dto) {
        // 회원 정보 조회
        UserDto user = userMapper.findUserById(userId);

        // 소셜 로그인 회원이 아닐 경우에만 비밀번호 검증
        if (user.getProvider() == null || user.getProvider().isBlank()) {
            String currentPasswordHash = userMapper.getCurrentPassword(userId);

            if (currentPasswordHash == null || !passwordEncoder.matches(dto.getPassword(), currentPasswordHash)) {
                throw new IllegalArgumentException("비밀번호가 일치하지 않습니다.");
            }
        }

        // 탈퇴 이력 저장
        userMapper.insertWithdrawal(userId, dto.getReasonCode(), dto.getReasonText(), "SELF");
        
        // 회원 상태 WITHDRAWN으로 변경
        int updated = userMapper.updateUserAsWithdrawn(userId);
        if (updated == 0) {
            throw new IllegalArgumentException("회원 탈퇴에 실패했습니다.");
        }
    }
}
