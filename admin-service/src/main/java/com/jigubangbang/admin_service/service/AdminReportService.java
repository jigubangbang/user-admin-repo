package com.jigubangbang.admin_service.service;

import com.jigubangbang.admin_service.chat_service.NotificationServiceClient;
import com.jigubangbang.admin_service.mapper.AdminReportMapper;
import com.jigubangbang.admin_service.mapper.BlindCountMapper;
import com.jigubangbang.admin_service.mapper.ContentMapper;
import com.jigubangbang.admin_service.model.AdminReportDto;
import com.jigubangbang.admin_service.model.chat_service.BlindNotificationRequestDto;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class AdminReportService {

    private final AdminReportMapper adminReportMapper;
    private final BlindCountMapper blindCountMapper;
    private final ContentMapper contentMapper;
    private final NotificationServiceClient notificationServiceClient;

    // 신고 목록 전체 조회
    public List<AdminReportDto> getAllReports() {
        return adminReportMapper.getAllReports();
    }

    // 신고 기각 
    public void keepReport(int reportId) {
        adminReportMapper.updateReportStatus(reportId, "KEPT");
    }

    // 신고 승인 
    public void blindReport(int reportId) {
        adminReportMapper.updateReportStatus(reportId, "BLINDED");
        AdminReportDto report = adminReportMapper.findReportById(reportId);
        blindTargetContent(report.getContentType(), report.getContentSubtype(), report.getContentId());
        blindCountMapper.increaseBlindCount(report.getTargetUserId());
        
        BlindNotificationRequestDto notification = BlindNotificationRequestDto.builder()
                .userId(report.getTargetUserId())
                .message("콘텐츠가 블라인드 처리되었습니다.\n자세한 사항은 1:1 문의를 통해 확인해 주세요.")
                .relatedUrl("/user/inquiry")
                .senderId(null)
                .build();

        notificationServiceClient.createBlindNotification(notification);
        
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

    // 신고 승인 철회
    @Transactional
    public void cancelBlindReport(int reportId) {
        AdminReportDto report = adminReportMapper.findReportById(reportId);

        if (!"BLINDED".equals(report.getReportStatus())) {
            throw new IllegalStateException("신고 승인 철회가 불가능합니다.");
        }

        adminReportMapper.updateReportStatus(reportId, "RESCIND");
        unblindTargetContent(report.getContentType(), report.getContentSubtype(), report.getContentId());
        blindCountMapper.decreaseBlindCount(report.getTargetUserId());
    }

    private void unblindTargetContent(String type, String subtype, int id) {
        switch (type) {
            case "POST" -> {
                switch (subtype) {
                    case "COMMUNITY" -> contentMapper.unblindBoardPost(id);
                    case "TRAVELFEED" -> contentMapper.unblindFeedPost(id);
                }
            }
            case "COMMENT" -> {
                switch (subtype) {
                    case "COMMUNITY" -> contentMapper.unblindBoardComment(id);
                    case "TRAVELFEED" -> contentMapper.unblindFeedComment(id);
                    case "TRAVELMATE" -> contentMapper.unblindMateComment(id);
                }
            }
            case "GROUP" -> {
                switch (subtype) {
                    case "TRAVELMATE" -> contentMapper.unblindMateGroup(id);
                    case "TRAVELINFO" -> contentMapper.unblindInfoGroup(id);
                }
            }
        }
    }
}
