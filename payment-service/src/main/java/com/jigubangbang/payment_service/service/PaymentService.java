package com.jigubangbang.payment_service.service;

import com.jigubangbang.payment_service.model.PaymentHistoryDto;

/**
 * 결제 관련 비즈니스 로직을 처리하는 서비스 인터페이스
 */
public interface PaymentService {

    /**
     * 결제 정보를 사전에 등록하고 merchant_uid를 생성하여 반환합니다.
     * @param paymentDto user_id, amount 등 사전 등록에 필요한 정보
     * @return 생성된 merchant_uid
     */
    String preparePayment(PaymentHistoryDto paymentDto);

    /**
     * 포트원 웹훅을 수신하여 결제 정보를 검증하고 후속 처리를 진행합니다.
     * @param impUid 포트원 결제 고유 ID
     * @param merchantUid 가맹점 주문번호
     * @return 처리된 결제 내역 정보
     */
    PaymentHistoryDto processWebhook(String impUid, String merchantUid);

    /**
     * 스케줄러에 의해 호출되어 만료가 임박한 구독에 대해 자동 결제를 실행합니다.
     */
    void processScheduledPayments();

}
