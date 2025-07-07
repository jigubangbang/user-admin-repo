package com.jigubangbang.user_service.model;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ChangeEmailDto {

    @Email(message = "올바른 이메일 형식으로 입력해 주세요.")
    private String email;

    @NotBlank(message = "인증 코드를 입력해 주세요.")
    private String emailCode;
}
