package com.jigubangbang.user_service.service;

import com.jigubangbang.user_service.mapper.ReportMapper;
import com.jigubangbang.user_service.model.CreateReportDto;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class ReportService {

    private final ReportMapper reportMapper;

    public void createReport(CreateReportDto dto) {
        // ETC일 경우 상세 사유가 비어있으면 예외
        if ("ETC".equalsIgnoreCase(dto.getReasonCode())) {
            if (dto.getReasonText() == null || dto.getReasonText().trim().isEmpty()) {
                throw new IllegalArgumentException("기타 사유 선택 시 상세 사유를 입력해야 합니다.");
            }
        }
        reportMapper.insertReport(dto);
    }
}
