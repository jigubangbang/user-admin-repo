package com.jigubangbang.admin_service.mapper;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface BlindCountMapper {
    void increaseBlindCount(@Param("userId") String userId);
    void decreaseBlindCount(@Param("userId") String userId);
}
