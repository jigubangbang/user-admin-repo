package com.jigubangbang.user_service.model;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class UserDto {
    private String id;     // user.user_id
    private String name;  
    private String role;  
    private String nickname;
    private String email;
    private String tel;
    private String profileImage;
    private String userStatus;
    private boolean isPremium;
}
