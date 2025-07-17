package com.jigubangbang.user_service.model;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class FindPwdRequestDto {
    private String userId;
    private String name;
    private String email;
}
