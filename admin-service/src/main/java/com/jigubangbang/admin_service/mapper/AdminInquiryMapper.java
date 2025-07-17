package com.jigubangbang.admin_service.mapper;

import com.jigubangbang.admin_service.model.AdminInquiryDto;
import java.util.List;

import org.apache.ibatis.annotations.Param;

public interface AdminInquiryMapper {
    // 전체 조회
    List<AdminInquiryDto> getAllInquiries();

    // 상세 조회 
    AdminInquiryDto getInquiryById(int inquiryId); 

    // 답변 등록
    int updateInquiryReply(@Param("inquiryId") int inquiryId, @Param("adminId") String adminId, @Param("reply") String reply);
}
