package com.jigubangbang.admin_service.model;

import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
public class AdminReportDto {

    private int id;                    // 신고 ID

    private String reporterId;         // 신고자 ID
    private String reporterNickname;   // 신고자 닉네임

    private String targetUserId;       // 신고 대상 ID
    private String targetNickname;     // 신고 대상 닉네임

    private String contentType;        // POST, COMMENT, GROUP
    private String contentSubtype;     // COMMUNITY, TRAVELFEED, TRAVELMATE, TRAVELINFO
    private int contentId;             // 신고 대상 컨텐츠 ID

    private String reasonCode;         // SEX, ABU, SPM, ILG, ETC
    private String reasonText;         // 기타 사유 텍스트 (선택)

    private String reportStatus;       // PENDING, KEPT, BLINDED
    private LocalDateTime reportedAt;  // 신고 일시
}
