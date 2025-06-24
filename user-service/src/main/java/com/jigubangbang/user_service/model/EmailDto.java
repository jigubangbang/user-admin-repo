package com.jigubangbang.user_service.model;

import java.time.LocalDateTime;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class EmailDto {
    private String email;
    private String code;
    private LocalDateTime expiresAt;
}
