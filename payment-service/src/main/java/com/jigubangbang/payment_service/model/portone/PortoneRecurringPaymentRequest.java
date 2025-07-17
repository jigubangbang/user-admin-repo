package com.jigubangbang.payment_service.model.portone;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;

/**
 * 포트원 '빌링키로 재결제' API 요청을 위한 DTO
 */
@Data
@Builder
public class PortoneRecurringPaymentRequest {

    // 빌링키
    @JsonProperty("customer_uid")
    private String customerUid;

    // 새로 생성한 주문번호
    @JsonProperty("merchant_uid")
    private String merchantUid;

    // 결제할 금액
    private BigDecimal amount;

    // 상품명
    private String name;

    // 웹훅(결과 통보) URL
    @JsonProperty("notice_url")
    private String noticeUrl;
}
