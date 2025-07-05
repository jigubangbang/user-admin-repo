package com.jigubangbang.user_service.service;

import com.jigubangbang.user_service.mapper.InquiryMapper;
import com.jigubangbang.user_service.model.CreateInquiryDto;
import com.jigubangbang.user_service.model.InquiryDto;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class InquiryService {

    private final InquiryMapper inquiryMapper;

    // 문의 등록
    public int createInquiry(String userId, CreateInquiryDto dto) {
        
        if (dto.getAttachments() != null && !dto.getAttachments().isEmpty()) {
            String attachmentStr = String.join(",", dto.getAttachments());
            dto.setAttachment(attachmentStr);
        }
        inquiryMapper.insertInquiry(userId, dto);
        return dto.getId();
    }

    // 사용자 본인의 문의 전체 목록
    public List<InquiryDto> getInquiriesByUser(String userId) {
        List<InquiryDto> list = inquiryMapper.selectInquiriesByUserId(userId);
        for (InquiryDto dto : list) {
            if (dto.getAttachment() != null && !dto.getAttachment().isBlank()) {
                List<String> attachments = Arrays.stream(dto.getAttachment().split(","))
                        .map(String::trim)
                        .collect(Collectors.toList());
                dto.setAttachments(attachments);
            }
        }
        return list;
    }

    // 문의 상세 조회
    public InquiryDto getInquiryDetail(int id, String userId) {
        InquiryDto inquiry = inquiryMapper.selectInquiryById(id);
        if (inquiry == null || !inquiry.getUserId().equals(userId)) {
            throw new IllegalArgumentException("해당 문의를 조회할 권한이 없습니다.");
        }

        if (inquiry.getAttachment() != null && !inquiry.getAttachment().isBlank()) {
            List<String> attachments = Arrays.stream(inquiry.getAttachment().split(","))
                    .map(String::trim)
                    .collect(Collectors.toList());
            inquiry.setAttachments(attachments);
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
