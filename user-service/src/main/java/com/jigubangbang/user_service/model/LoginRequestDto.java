package com.jigubangbang.user_service.model;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class LoginRequestDto {
    private String userId;
    private String password;
}
