package com.jigubangbang.admin_service.mapper;

import com.jigubangbang.admin_service.model.AdminReportDto;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface AdminReportMapper {
    // 신고 목록 전체 조회
    List<AdminReportDto> getAllReports();

    // 신고 상세 조회 (신고 승인 처리 시 필요)
    AdminReportDto findReportById(@Param("id") int id);

    // 신고 상태 업데이트 (KEPT / BLINDED)
    void updateReportStatus(@Param("id") int id, @Param("status") String status);
}
