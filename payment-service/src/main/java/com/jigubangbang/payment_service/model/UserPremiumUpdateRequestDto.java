package com.jigubangbang.payment_service.model;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class UserPremiumUpdateRequestDto {
    private Boolean premium;
    private String customerUid;
}
