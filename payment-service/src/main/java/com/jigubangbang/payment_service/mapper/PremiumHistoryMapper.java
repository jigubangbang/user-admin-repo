package com.jigubangbang.payment_service.mapper;

import com.jigubangbang.payment_service.model.PremiumHistoryDto;
import org.apache.ibatis.annotations.Mapper;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * premium_history 테이블에 접근하기 위한 MyBatis Mapper 인터페이스
 */
@Mapper
public interface PremiumHistoryMapper {

    /**
     * 새로운 프리미엄 구독 내역을 저장합니다.
     * @param premiumHistoryDto 저장할 구독 정보
     * @return 영향을 받은 행의 수
     */
    int createPremiumHistory(PremiumHistoryDto premiumHistoryDto);

    /**
     * 특정 사용자의 현재 활성화된 프리미엄 구독 내역을 조회합니다.
     * @param userId 조회할 사용자 ID
     * @return 활성화된 구독 정보 (Optional)
     */
    Optional<PremiumHistoryDto> findActiveByUserId(String userId);

    /**
     * 프리미엄 구독 내역을 수정합니다. (예: 기존 구독을 비활성화 처리)
     * @param premiumHistoryDto 수정할 구독 정보 (id, isActive 등)
     * @return 영향을 받은 행의 수
     */
    int updatePremiumHistory(PremiumHistoryDto premiumHistoryDto);

    /**
     * 구독 만료가 임박한 활성 사용자 목록을 조회합니다.
     * 자동 결제 스케줄러에서 사용됩니다.
     * @param expirationDate 만료 기준 시각
     * @return 구독 만료 예정인 사용자 목록
     */
    List<PremiumHistoryDto> findExpiringSubscriptions(LocalDateTime expirationDate);

    /**
     * 특정 사용자의 가장 최근 프리미엄 구독 내역을 조회합니다.
     * 활성/비활성 여부와 관계없이 가장 최근의 기록을 반환합니다.
     * @param userId 조회할 사용자 ID
     * @return 가장 최근의 구독 정보 (Optional)
     */
    Optional<PremiumHistoryDto> findLatestByUserId(String userId);
}
