package com.jigubangbang.user_service.mapper;

import com.jigubangbang.user_service.model.CreateReportDto;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface ReportMapper {
    // 신고 등록
    void insertReport(CreateReportDto dto);
}
