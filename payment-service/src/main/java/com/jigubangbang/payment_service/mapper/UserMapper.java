package com.jigubangbang.payment_service.mapper;

import com.jigubangbang.payment_service.model.UserPremiumUpdateRequestDto;
import com.jigubangbang.payment_service.model.UserResponseDto;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface UserMapper {
    UserResponseDto findById(String userId);
    void updateUserPremium(@Param("userId") String userId, @Param("request") UserPremiumUpdateRequestDto request);
}
