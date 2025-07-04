package com.jigubangbang.user_service.service;

import com.jigubangbang.user_service.mapper.InquiryMapper;
import com.jigubangbang.user_service.model.CreateInquiryDto;
import com.jigubangbang.user_service.model.InquiryDto;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class InquiryService {

    private final InquiryMapper inquiryMapper;

    // 문의 등록
    public int createInquiry(String userId, CreateInquiryDto dto) {
        inquiryMapper.insertInquiry(userId, dto);
        return dto.getId();
    }

    // 사용자 본인의 문의 전체 목록
    public List<InquiryDto> getInquiriesByUser(String userId) {
        return inquiryMapper.selectInquiriesByUserId(userId);
    }

    // 문의 상세 조회
    public InquiryDto getInquiryDetail(int id, String userId) {
        InquiryDto inquiry = inquiryMapper.selectInquiryById(id);
        if (inquiry == null || !inquiry.getUserId().equals(userId)) {
            throw new IllegalArgumentException("해당 문의를 조회할 권한이 없습니다.");
        }
        return inquiry;
    }

    // 문의 수정
    public void updateInquiry(int id, String userId, CreateInquiryDto dto) {
        int updated = inquiryMapper.updateInquiryByUser(id, userId, dto);
        if (updated == 0) {
            throw new IllegalArgumentException("문의 수정에 실패했습니다.");
        }
    }

    // 문의 삭제
    public void deleteInquiry(int id, String userId) {
        int deleted = inquiryMapper.deleteInquiryByUser(id, userId);
        if (deleted == 0) {
            throw new IllegalArgumentException("문의 삭제에 실패했습니다.");
        }
    }
}
