package com.jigubangbang.admin_service.model.chat_service;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class InquiryNotificationRequestDto {
    private String userId;
    private String title;
    private String message;
    private String relatedUrl;
    private String senderId;
    private String nickname;
}