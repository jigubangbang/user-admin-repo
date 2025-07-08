package com.jigubangbang.admin_service.model.chat_service;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class BadgeNotificationRequestDto {
    
    private String userId;
    private String badgeName;
    private String message;
    private int badgeId;
    private String relatedUrl;
    private String senderId;
    private String senderProfileImage;

}
