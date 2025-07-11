package com.jigubangbang.payment_service.mapper;

import com.jigubangbang.payment_service.model.PaymentHistoryDto;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Mapper;

import org.apache.ibatis.annotations.Param;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * payment 테이블에 접근하기 위한 MyBatis Mapper 인터페이스
 */
@Mapper
public interface PaymentHistoryMapper {

    /**
     * 특정 사용자의 모든 결제 내역을 조회합니다.
     * @param userId 사용자 ID
     * @return 결제 내역 목록
     */
    List<PaymentHistoryDto> findByUserId(String userId);

    /**
     * 결제 내역을 저장합니다.
     * @param paymentHistoryDto 저장할 결제 정보
     * @return 영향을 받은 행의 수
     */
    int createPaymentHistory(PaymentHistoryDto paymentHistoryDto);

    /**
     * 가맹점 주문번호(merchant_uid)로 결제 내역을 조회합니다.
     * @param merchantUid 조회할 주문번호
     * @return 결제 내역 정보 (Optional)
     */
    Optional<PaymentHistoryDto> findByMerchantUid(String merchantUid);

    /**
     * 결제 상태를 업데이트합니다.
     * @param paymentHistoryDto 업데이트할 결제 정보 (merchantUid, payStatus, impUid 등)
     * @return 영향을 받은 행의 수
     */
    int updatePaymentStatus(PaymentHistoryDto paymentHistoryDto);
    
    /**
     * imp_uid와 paid_at을 업데이트합니다.
     * @param paymentHistoryDto 업데이트할 정보 (merchantUid, impUid)
     * @return 영향을 받은 행의 수
     */
    int updatePaymentImpUid(PaymentHistoryDto paymentHistoryDto);

    /**
     * 가맹점 주문번호(merchant_uid)로 결제 내역을 삭제합니다.
     * @param merchantUid 삭제할 주문번호
     * @return 영향을 받은 행의 수
     */
    @Delete("DELETE FROM payment WHERE merchant_uid = #{merchantUid}")
    int deleteByMerchantUid(String merchantUid);

    /**
     * 특정 시간 이전에 생성된 'READY' 상태의 결제 내역을 삭제합니다.
     * @param cutoffTime 기준 시간
     * @return 삭제된 행의 수
     */
    @Delete("DELETE FROM payment WHERE pay_status = 'READY' AND paid_at < #{cutoffTime}")
    int deleteOldReadyPayments(@Param("cutoffTime") LocalDateTime cutoffTime);

}
