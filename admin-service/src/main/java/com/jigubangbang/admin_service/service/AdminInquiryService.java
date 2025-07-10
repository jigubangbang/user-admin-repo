package com.jigubangbang.admin_service.service;

import com.jigubangbang.admin_service.mapper.AdminInquiryMapper;
import com.jigubangbang.admin_service.model.AdminInquiryDto;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class AdminInquiryService {

    private final AdminInquiryMapper adminInquiryMapper;

    // 전체 조회
    public List<AdminInquiryDto> getInquiryList() {
        return adminInquiryMapper.getAllInquiries();
    }

    // 상세 조회
    public AdminInquiryDto getInquiryDetail(int inquiryId) {
        return adminInquiryMapper.getInquiryById(inquiryId);
    }
}
