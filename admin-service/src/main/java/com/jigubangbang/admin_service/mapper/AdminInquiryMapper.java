package com.jigubangbang.admin_service.mapper;

import com.jigubangbang.admin_service.model.AdminInquiryDto;
import java.util.List;

public interface AdminInquiryMapper {
    // 전체 조회
    List<AdminInquiryDto> getAllInquiries();

    // 상세 조회 
    AdminInquiryDto getInquiryById(int inquiryId); 
}
