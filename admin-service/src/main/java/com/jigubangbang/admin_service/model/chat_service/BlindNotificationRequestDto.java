package com.jigubangbang.admin_service.model.chat_service;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class BlindNotificationRequestDto {
    private String userId;
    private String message;
    private String relatedUrl;
    private String senderId;
}
