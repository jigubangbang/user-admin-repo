package com.jigubangbang.user_service.model;

import java.time.LocalDateTime;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class FindPwdResponseDto {
    private String userId;
    private String provider;
    private LocalDateTime tempPwdAt;
    private boolean isLimited;
}
