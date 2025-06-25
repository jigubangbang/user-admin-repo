package com.jigubangbang.user_service.mapper;

import com.jigubangbang.user_service.model.EmailDto;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface EmailMapper {

    // 인증 코드 저장
    void insertOrUpdateCode(EmailDto dto);

    // 이메일로 인증 정보 조회
    EmailDto findByEmail(String email);

    // 인증 성공 처리
    void markEmailAsVerified(String email);
}
