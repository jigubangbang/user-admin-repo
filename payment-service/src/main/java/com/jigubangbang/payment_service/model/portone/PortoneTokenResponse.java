package com.jigubangbang.payment_service.model.portone;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;
import lombok.NoArgsConstructor;

// 포트원 토큰 발급 API의 전체 응답 구조를 나타내는 클래스
@Data
@NoArgsConstructor
public class PortoneTokenResponse {
    private int code;
    private String message;
    private TokenInfo response;

    // 실제 토큰 정보가 담긴 내부 response 객체
    @Data
    @NoArgsConstructor
    public static class TokenInfo {
        @JsonProperty("access_token")
        private String accessToken;

        @JsonProperty("expired_at")
        private long expiredAt;

        @JsonProperty("now")
        private long now;
    }
}
