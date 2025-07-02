package com.jigubangbang.payment_service.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PaymentHistoryDto {

    private Long id;                // 결제 고유 ID (PK)
    private String userId;          // 결제한 사용자 ID (FK)
    private String impUid;          // 포트원 결제 고유 ID
    private String merchantUid;     // 주문 고유 ID (내부 트래킹용)
    private Integer amount;         // 결제 금액
    private String payStatus;       // 결제 상태 ('PAID', 'CANCELLED')
    private LocalDateTime paidAt;   // 결제 시각
    private LocalDateTime cancelledAt; // 환불 처리 시각
}
