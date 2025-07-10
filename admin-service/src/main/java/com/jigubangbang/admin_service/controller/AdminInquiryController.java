package com.jigubangbang.admin_service.controller;

import com.jigubangbang.admin_service.model.AdminInquiryDto;
import com.jigubangbang.admin_service.service.AdminInquiryService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/admin/inquiries")
@RequiredArgsConstructor
public class AdminInquiryController {

    private final AdminInquiryService adminInquiryService;

    // 전체 조회
    @GetMapping
    public List<AdminInquiryDto> getAllInquiries() {
        return adminInquiryService.getInquiryList();
    }

    // 상세 조회
    @GetMapping("/{inquiryId}")
    public AdminInquiryDto getInquiryDetail(@PathVariable int inquiryId) {
        return adminInquiryService.getInquiryDetail(inquiryId);
    }
}
