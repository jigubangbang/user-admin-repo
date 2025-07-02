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
public class PremiumHistoryDto {

    private Long id;                    // 프리미엄 이력 고유 ID (PK)
    private String userId;              // 프리미엄 구독자 (FK)
    private LocalDateTime startDate;    // 프리미엄 시작 시각
    private LocalDateTime endDate;      // 프리미엄 종료 시각
    private Boolean isActive;           // 현재 프리미엄 유지 여부
}
