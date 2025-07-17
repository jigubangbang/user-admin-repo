package com.jigubangbang.user_service.model;

import java.sql.Timestamp;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class EmailDto {
    private String email;
    private String code;
    private Timestamp expiresAt;
    private Boolean isVerified; 
}
