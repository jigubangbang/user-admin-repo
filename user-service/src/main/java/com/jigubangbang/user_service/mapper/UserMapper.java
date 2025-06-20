package com.jigubangbang.user_service.mapper;

import com.jigubangbang.user_service.model.AuthDto;
import com.jigubangbang.user_service.model.UserDto;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface UserMapper {

    // Security 인증용 사용자 정보 조회
    AuthDto findAuthById(String userId);

    // 일반 사용자 정보 조회
    UserDto findUserById(String userId);
}
