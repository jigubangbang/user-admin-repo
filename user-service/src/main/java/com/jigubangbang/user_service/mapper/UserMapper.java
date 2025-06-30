package com.jigubangbang.user_service.mapper;

import com.jigubangbang.user_service.model.AuthDto;
import com.jigubangbang.user_service.model.FindIdResponseDto;
import com.jigubangbang.user_service.model.RegisterRequestDto;
import com.jigubangbang.user_service.model.UserDto;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface UserMapper {

    // Security 인증용 사용자 정보 조회
    AuthDto findAuthById(String userId);

    // 일반 사용자 정보 조회
    UserDto findUserById(String userId);

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

}
