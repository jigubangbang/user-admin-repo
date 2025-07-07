package com.jigubangbang.user_service.mapper;

import com.jigubangbang.user_service.model.AuthDto;
import com.jigubangbang.user_service.model.FindIdResponseDto;
import com.jigubangbang.user_service.model.FindPwdResponseDto;
import com.jigubangbang.user_service.model.RegisterRequestDto;
import com.jigubangbang.user_service.model.UpdateUserDto;
import com.jigubangbang.user_service.model.UserDto;

import java.time.LocalDateTime;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface UserMapper {

    // Security 인증용 사용자 정보 조회
    AuthDto findAuthById(String userId);

    // 일반 사용자 정보 조회
    UserDto findUserById(String userId);

    // 정지 회원 복구
    int restoreUserToActive(String userId);

    // 회원가입
    void insertUser(RegisterRequestDto dto);

    // 아이디 중복 체크
    boolean existsByUserId(String userId);

    // 이메일 중복 체크
    boolean existsByEmail(String email);

    // 이메일로 사용자 조회 (소셜 로그인)
    UserDto findByEmail(String email);

    // 아이디 찾기
    FindIdResponseDto findByNameAndEmail(String name, String email);

    // 비밀번호 찾기
    FindPwdResponseDto findByUserIdNameEmail(String userId, String name, String email);

    // 임시 비밀번호 발급
    void updatePasswordAndTempPwdAt(String userId, String password, LocalDateTime tempPwdAt);

    // 회원 정보 수정
    int updateUserInfo(@Param("userId") String userId, @Param("dto") UpdateUserDto dto);

    // 닉네임 최근 변경일 조회
    LocalDateTime getNicknameUpdatedAt(String userId);

    // 현재 비밀번호 조회
    String getCurrentPassword(String userId);

    // 비밀번호 업데이트
    int updatePassword(@Param("userId") String userId, @Param("newPassword") String newPassword);

    // 이메일 변경
    int updateEmail(@Param("userId") String userId, @Param("newEmail") String newEmail);

    // 탈퇴 이력 저장
    void insertWithdrawal(
            @Param("userId") String userId,
            @Param("reasonCode") String reasonCode,
            @Param("reasonText") String reasonText,
            @Param("withdrawalType") String withdrawalType);

    // 회원 상태 WITHDRAWN으로 변경
    int updateUserAsWithdrawn(@Param("userId") String userId);

    // 상태 및 정지 기간 설정
    int updateStatusAndBannedUntil(@Param("userId") String userId, @Param("status") String status, @Param("bannedUntil") LocalDateTime bannedUntil);

    // 마지막 블라인드 횟수 저장
    int updateLastBlindCount(@Param("userId") String userId, @Param("lastBlindCount") int lastBlindCount);

}
