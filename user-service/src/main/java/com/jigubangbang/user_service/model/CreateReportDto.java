package com.jigubangbang.user_service.model;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class CreateReportDto {

    @NotBlank
    private String reporterId;

    @NotBlank
    private String targetUserId;

    @NotBlank
    private String contentType;      // POST, COMMENT, GROUP

    @NotBlank
    private String contentSubtype;   // COMMUNITY, TRAVELFEED, TRAVELMATE, TRAVELINFO

    @NotNull
    private Integer contentId;

    @NotBlank
    private String reasonCode;       // SEX, ABU, SPM, ILG, ETC

    private String reasonText;       // ETC일 경우 사용자 입력
}
