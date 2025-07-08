package com.jigubangbang.user_service.model;

import java.time.LocalDateTime;

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
    private String provider;
    private LocalDateTime nicknameUnlockAt;
    private int blindCount;
    private int lastBlindCount; 
    private LocalDateTime bannedUntil; 
}
