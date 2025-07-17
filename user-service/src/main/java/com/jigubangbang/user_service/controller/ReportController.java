package com.jigubangbang.user_service.controller;

import com.jigubangbang.user_service.model.CreateReportDto;
import com.jigubangbang.user_service.service.ReportService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/user")
@RequiredArgsConstructor
public class ReportController {

    private final ReportService reportService;

    // 신고 등록
    @PostMapping("/reports")
    public ResponseEntity<String> createReport(@Valid @RequestBody CreateReportDto dto) {
        try {
            reportService.createReport(dto);
            return ResponseEntity.ok("신고가 접수되었습니다.");
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }
}
