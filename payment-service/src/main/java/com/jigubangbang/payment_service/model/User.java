package com.jigubangbang.payment_service.model;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class User {
    private String userId;
    private String password;
    private String name;
    private String nickname;
    private String email;
    private String tel;
    private String role;
    // user-service Dto 처럼 추가 작성 필요
}
