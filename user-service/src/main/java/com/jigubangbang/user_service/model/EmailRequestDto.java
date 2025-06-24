package com.jigubangbang.user_service.model;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class EmailRequestDto {
    private String email;
    private String code;
}
