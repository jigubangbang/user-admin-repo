package com.jigubangbang.user_service.service;

import com.jigubangbang.user_service.mapper.InquiryMapper;
import com.jigubangbang.user_service.model.CreateInquiryDto;
import com.jigubangbang.user_service.model.InquiryDto;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
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
        List<InquiryDto> list = inquiryMapper.selectInquiriesByUserId(userId);
        for (InquiryDto dto : list) {
            if (dto.getAttachment() != null && !dto.getAttachment().isBlank()) {
                dto.setAttachments(parseJsonToAttachments(dto.getAttachment()));
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
            inquiry.setAttachments(parseJsonToAttachments(inquiry.getAttachment()));
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

    // JSON 문자열을 AttachmentInfo 리스트로 파싱
    private List<InquiryDto.AttachmentInfo> parseJsonToAttachments(String jsonString) {
        List<InquiryDto.AttachmentInfo> attachments = new ArrayList<>();
        if (jsonString == null || jsonString.trim().isEmpty()) {
            return attachments;
        }

        try {
            String content = jsonString.trim();
            if (content.startsWith("[") && content.endsWith("]")) {
                content = content.substring(1, content.length() - 1);
            }

            if (content.trim().isEmpty()) {
                return attachments;
            }

            String[] objects = content.split("\\},\\{");
            for (int i = 0; i < objects.length; i++) {
                String obj = objects[i];

                if (i == 0 && obj.startsWith("{")) {
                    obj = obj.substring(1);
                }
                if (i == objects.length - 1 && obj.endsWith("}")) {
                    obj = obj.substring(0, obj.length() - 1);
                }

                InquiryDto.AttachmentInfo info = new InquiryDto.AttachmentInfo();

                // originalName 추출
                int nameStart = obj.indexOf("\"originalName\":\"") + 16;
                int nameEnd = obj.indexOf("\",\"url\":");
                if (nameStart > 15 && nameEnd > nameStart) {
                    String originalName = obj.substring(nameStart, nameEnd);
                    info.setOriginalName(originalName);
                }

                // url 추출
                int urlStart = obj.indexOf("\"url\":\"") + 7;
                int urlEnd = obj.lastIndexOf("\"");
                if (urlStart > 6 && urlEnd > urlStart) {
                    String url = obj.substring(urlStart, urlEnd);
                    info.setUrl(url);
                }

                attachments.add(info);
            }
        } catch (Exception e) {
            System.err.println("JSON 파싱 오류: " + e.getMessage());
            e.printStackTrace();
        }

        return attachments;
    }
}
