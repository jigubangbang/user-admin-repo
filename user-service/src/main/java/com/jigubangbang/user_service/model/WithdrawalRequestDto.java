package com.jigubangbang.user_service.model;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class WithdrawalRequestDto {

    @NotBlank(message = "WITHDRAWAL_REASON_REQUIRED")
    @Pattern(
        regexp = "REJOIN|BADUX|CONTENT|PRIVACY|NOUSE|ETC",
        message = "WITHDRAWAL_REASON_INVALID"
    )
    private String reasonCode;
    private String reasonText;
}
