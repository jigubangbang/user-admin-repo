package com.jigubangbang.admin_service.controller;

import com.jigubangbang.admin_service.model.AdminReportDto;
import com.jigubangbang.admin_service.service.AdminReportService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/admin/reports")
@RequiredArgsConstructor
public class AdminReportController {

    private final AdminReportService adminReportService;

    // 신고 목록 조회
    @GetMapping
    public ResponseEntity<List<AdminReportDto>> getAllReports() {
        List<AdminReportDto> reports = adminReportService.getAllReports();
        return ResponseEntity.ok(reports);
    }

    // 신고 기각 처리
    @PutMapping("/{reportId}/keep")
    public ResponseEntity<String> keepReport(@PathVariable("reportId") int reportId) {
        adminReportService.keepReport(reportId);
        return ResponseEntity.ok("신고가 기각되었습니다.");
    }

    // 신고 승인 처리
    @PutMapping("/{reportId}/blind")
    public ResponseEntity<String> blindReport(@PathVariable("reportId") int reportId) {
        adminReportService.blindReport(reportId);
        return ResponseEntity.ok("신고가 승인되었습니다.");
    }

    // 신고 승인 철회 처리
    @PutMapping("/{reportId}/unblind")
    public ResponseEntity<String> cancelBlindReport(@PathVariable("reportId") int reportId) {
        try {
            adminReportService.cancelBlindReport(reportId);
            return ResponseEntity.ok("신고 승인 철회 및 콘텐츠가 복구되었습니다.");
        } catch (IllegalStateException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }
}
