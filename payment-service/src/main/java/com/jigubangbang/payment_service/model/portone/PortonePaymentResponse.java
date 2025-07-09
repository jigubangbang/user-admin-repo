package com.jigubangbang.payment_service.model.portone;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

// 포트원 결제 정보 조회 API의 전체 응답 구조를 나타내는 클래스
@Data
@NoArgsConstructor
public class PortonePaymentResponse {
    private int code;
    private String message;
    private PaymentInfo response;

    // 실제 결제 정보가 담긴 내부 response 객체
    @Data
    @NoArgsConstructor
    public static class PaymentInfo {
        @JsonProperty("imp_uid")
        private String impUid;

        @JsonProperty("merchant_uid")
        private String merchantUid;

        @JsonProperty("amount")
        private BigDecimal amount; // 금액은 정수(Integer)가 아닌 BigDecimal을 사용하는 것이 안전합니다.

        @JsonProperty("status")
        private String status; // 예: "paid", "ready", "failed", "cancelled"

        @JsonProperty("customer_uid")
        private String customerUid; // 빌링키 (자동결제용)

        @JsonProperty("pay_method")
        private String payMethod; // 결제 수단

        @JsonProperty("card_name")
        private String cardName; // 카드사

        @JsonProperty("card_number")
        private String cardNumber; // 마스킹된 카드 번호
    }
}
