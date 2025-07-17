package com.jigubangbang.user_service.model;

import jakarta.validation.constraints.*;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class UpdateUserDto {
    @Pattern(regexp = "^[가-힣]{2,6}$", message = "이름은 2~6자의 한글만 입력 가능합니다.")
    private String name;

    @Pattern(regexp = "^[a-zA-Z0-9가-힣]{1,10}$", message = "닉네임은 1~10자의 한글, 영문, 숫자만 입력 가능합니다.")
    private String nickname;

    @Pattern(regexp = "^\\d{3}-\\d{3,4}-\\d{4}$", message = "올바른 전화번호 형식으로 입력해 주세요.")
    private String tel;
}
