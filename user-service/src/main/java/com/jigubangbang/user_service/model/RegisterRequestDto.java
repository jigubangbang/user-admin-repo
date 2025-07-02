package com.jigubangbang.user_service.model;

import jakarta.validation.constraints.*;
import lombok.Data;

@Data
public class RegisterRequestDto {

    @NotBlank
    @Pattern(regexp = "^[a-zA-Z0-9]{6,12}$", 
             message = "아이디는 6~12자의 영문, 숫자만 입력 가능합니다")
    private String userId;

    @NotBlank
    @Pattern(regexp = "^(?=.*[a-zA-Z])(?=.*\\d)(?=.*[!@#\\-_$%^&*])[A-Za-z\\d!@#\\-_$%^&*]{8,20}$",
             message = "비밀번호는 8~20자의 영문, 숫자, 특수문자(~!@#$%^&*)를 모두 포함해야 합니다.")
    private String password;

    @NotBlank
    private String confirmPassword; 

    @NotBlank
    @Pattern(regexp = "^[가-힣]{2,6}$", 
             message = "이름은 2~6자의 한글만 입력 가능합니다.")
    private String name;

    @NotBlank
    @Pattern(regexp = "^[a-zA-Z0-9가-힣]{1,10}$", 
             message = "닉네임은 1~10자의 한글, 영문, 숫자만 입력 가능합니다.")
    private String nickname;

    @NotBlank
    @Email(message = "올바른 이메일 형식으로 입력해 주세요.")
    private String email;

    @NotBlank
    @Pattern(regexp = "^[0-9]{3}-[0-9]{4}-[0-9]{4}$", 
             message = "올바른 전화번호 형식으로 입력해 주세요")
    private String tel;

    @AssertTrue(message = "회원가입을 위해 필수 약관에 동의해 주세요.")
    private Boolean agreedRequired;

    private Boolean agreedOptional;
    
    private String provider;     // ex: "kakao", "google"
    private String providerId;   // 소셜 플랫폼 고유 식별자
}
