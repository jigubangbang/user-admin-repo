package com.jigubangbang.admin_service.service;

import com.jigubangbang.admin_service.mapper.AdminReportMapper;
import com.jigubangbang.admin_service.mapper.BlindCountMapper;
import com.jigubangbang.admin_service.mapper.ContentMapper;
import com.jigubangbang.admin_service.model.AdminReportDto;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class AdminReportService {

    private final AdminReportMapper adminReportMapper;
    private final BlindCountMapper blindCountMapper;
    private final ContentMapper contentMapper;

    // 신고 목록 전체 조회
    public List<AdminReportDto> getAllReports() {
        return adminReportMapper.getAllReports();
    }

    // 신고 기각 처리
    public void keepReport(int reportId) {
        adminReportMapper.updateReportStatus(reportId, "KEPT");
    }

    // 신고 승인 처리
    public void blindReport(int reportId) {
        adminReportMapper.updateReportStatus(reportId, "BLINDED");
        AdminReportDto report = adminReportMapper.findReportById(reportId);
        blindTargetContent(report.getContentType(), report.getContentSubtype(), report.getContentId());
        blindCountMapper.increaseBlindCount(report.getTargetUserId());
    }

    private void blindTargetContent(String type, String subtype, int id) {
        switch (type) {
            case "POST" -> {
                switch (subtype) {
                    case "COMMUNITY" -> contentMapper.blindBoardPost(id);
                    case "TRAVELFEED" -> contentMapper.blindFeedPost(id);
                }
            }
            case "COMMENT" -> {
                switch (subtype) {
                    case "COMMUNITY" -> contentMapper.blindBoardComment(id);
                    case "TRAVELFEED" -> contentMapper.blindFeedComment(id);
                    case "TRAVELMATE" -> contentMapper.blindMateComment(id);
                }
            }
            case "GROUP" -> {
                switch (subtype) {
                    case "TRAVELMATE" -> contentMapper.blindMateGroup(id);
                    case "TRAVELINFO" -> contentMapper.blindInfoGroup(id);
                }
            }
        }
    }
}
