package com.jigubangbang.user_service.model;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ChangePwdDto {

    @NotBlank(message = "현재 비밀번호를 입력해 주세요.")
    private String currentPassword;

    @NotBlank(message = "새 비밀번호를 입력해 주세요.")
    @Pattern(
        regexp = "^(?=.*[a-zA-Z])(?=.*\\d)(?=.*[!@#\\-_$%^&*])[A-Za-z\\d!@#\\-_$%^&*]{8,20}$",
        message = "비밀번호는 8~20자의 영문, 숫자, 특수문자를 모두 포함해야 합니다."
    )
    private String newPassword;
}
