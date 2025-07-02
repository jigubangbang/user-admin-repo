package com.jigubangbang.payment_service.service;

import com.jigubangbang.payment_service.client.PortoneClient;
import com.jigubangbang.payment_service.client.UserServiceClient;
import com.jigubangbang.payment_service.mapper.PaymentHistoryMapper;
import com.jigubangbang.payment_service.mapper.PremiumHistoryMapper;
import com.jigubangbang.payment_service.model.PaymentHistoryDto;
import com.jigubangbang.payment_service.model.PremiumHistoryDto;
import com.jigubangbang.payment_service.model.UserResponseDto;
import com.jigubangbang.payment_service.model.portone.PortonePaymentResponse;
import com.jigubangbang.payment_service.model.portone.PortoneRecurringPaymentRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Service
@Slf4j
@RequiredArgsConstructor
public class PaymentServiceImpl implements PaymentService {

    private final PaymentHistoryMapper paymentHistoryMapper;
    private final PremiumHistoryMapper premiumHistoryMapper;
    private final PortoneClient portoneClient;
    private final UserServiceClient userServiceClient;

    @Override
    @Transactional
    public String preparePayment(PaymentHistoryDto request) {
        String merchantUid = "order_" + UUID.randomUUID().toString().replace("-", "");
        PaymentHistoryDto newPayment = PaymentHistoryDto.builder()
                .userId(request.getUserId())
                .merchantUid(merchantUid)
                .amount(request.getAmount())
                .payStatus("READY")
                .build();
        paymentHistoryMapper.createPaymentHistory(newPayment);
        log.info("결제 정보 사전 등록 완료: merchant_uid={}", merchantUid);
        return merchantUid;
    }

    @Override
    @Transactional
    public PaymentHistoryDto processWebhook(String impUid, String merchantUid) {
        log.info("웹훅 처리 시작: imp_uid={}, merchant_uid={}", impUid, merchantUid);
        try {
            PaymentHistoryDto order = paymentHistoryMapper.findByMerchantUid(merchantUid)
                    .orElseThrow(() -> new RuntimeException("존재하지 않는 주문입니다. merchant_uid=" + merchantUid));
            String accessToken = portoneClient.getAccessToken();
            PortonePaymentResponse.PaymentInfo paymentInfo = portoneClient.getPaymentInfo(impUid, accessToken);

            if (paymentInfo.getAmount().compareTo(new BigDecimal(order.getAmount())) != 0) {
                throw new RuntimeException("결제 금액이 일치하지 않습니다.");
            }

            if ("paid".equals(paymentInfo.getStatus())) {
                log.info("유효한 결제 확인. 결제 상태: {}", paymentInfo.getStatus());
                order.setPayStatus("PAID");
                order.setImpUid(impUid);
                paymentHistoryMapper.updatePaymentStatus(order);
                updatePremiumSubscription(order.getUserId());
                log.info("결제 및 구독 처리 최종 완료. userId={}", order.getUserId());
                return order;
            } else {
                log.warn("결제가 완료되지 않았습니다. 상태: {}", paymentInfo.getStatus());
                return null;
            }
        } catch (Exception e) {
            log.error("웹훅 처리 중 심각한 오류 발생", e);
            throw new RuntimeException("웹훅 처리 중 오류가 발생했습니다.", e);
        }
    }

    /**
     * 스케줄러에 의해 호출되어 만료가 임박한 구독에 대해 자동 결제를 실행합니다.
     */
    @Override
    @Transactional
    public void processScheduledPayments() {
        log.info("===== 자동 결제 스케줄러 실행 시작 =====");

        // 1. 구독 만료가 하루 남은 사용자 목록 조회
        LocalDateTime tomorrow = LocalDateTime.now().plusDays(1);
        List<PremiumHistoryDto> targets = premiumHistoryMapper.findExpiringSubscriptions(tomorrow);

        if (targets.isEmpty()) {
            log.info("자동 결제 대상이 없습니다.");
            log.info("===== 자동 결제 스케줄러 실행 종료 =====");
            return;
        }

        log.info("자동 결제 대상 {}건 발견", targets.size());

        // 2. 여러 건을 처리해야 하므로, API 인증 토큰을 미리 한 번만 발급받습니다.
        String accessToken = portoneClient.getAccessToken();

        for (PremiumHistoryDto subscription : targets) {
            String userId = subscription.getUserId();
            log.info("사용자 [{}]에 대한 자동 결제를 시도합니다.", userId);

            try {
                // 3. Feign Client를 사용해 user-service에서 사용자 정보(빌링키 포함) 조회
                UserResponseDto userInfo = userServiceClient.getUserInfo(userId);
                String billingKey = userInfo.getCustomerUid();

                if (billingKey == null || billingKey.isBlank()) {
                    log.warn("사용자 [{}]의 빌링키(customer_uid)가 존재하지 않아 자동 결제를 건너뜁니다.", userId);
                    continue; // 다음 사용자로 넘어감
                }

                // 4. 결제할 금액 및 상품명 설정 (실제로는 DB의 상품 테이블 등에서 조회)
                BigDecimal amountToPay = new BigDecimal(9900);
                String productName = "월간 프리미엄 구독 자동 연장";

                // 5. '사전 등록' 로직을 재사용하여 자동 결제 건을 미리 DB에 저장
                PaymentHistoryDto autoPaymentDto = PaymentHistoryDto.builder()
                        .userId(userId)
                        .amount(amountToPay.intValue())
                        .build();
                String newMerchantUid = this.preparePayment(autoPaymentDto);

                // 6. 포트원에 자동 결제(재결제) 요청
                PortoneRecurringPaymentRequest recurringRequest = PortoneRecurringPaymentRequest.builder()
                        .customerUid(billingKey)
                        .merchantUid(newMerchantUid)
                        .amount(amountToPay)
                        .name(productName)
                        .build();
                
                // API를 호출합니다. 결과는 웹훅으로 비동기 수신되므로 여기서의 리턴값은 참고용입니다.
                portoneClient.requestRecurringPayment(recurringRequest, accessToken);

            } catch (Exception e) {
                log.error("사용자 [{}]의 자동 결제 처리 중 오류 발생", userId, e);
                // TODO: 결제 실패 시 사용자에게 알림을 보내거나, 관리자에게 보고하는 로직 추가
            }
        }
        log.info("===== 자동 결제 스케줄러 실행 종료 =====");
    }

    private void updatePremiumSubscription(String userId) {
        premiumHistoryMapper.findActiveByUserId(userId).ifPresent(activeSubscription -> {
            activeSubscription.setIsActive(false);
            activeSubscription.setEndDate(LocalDateTime.now());
            premiumHistoryMapper.updatePremiumHistory(activeSubscription);
            log.info("기존 구독(id={})을 비활성화 처리했습니다.", activeSubscription.getId());
        });

        PremiumHistoryDto newSubscription = PremiumHistoryDto.builder()
                .userId(userId)
                .startDate(LocalDateTime.now())
                .endDate(LocalDateTime.now().plusMonths(1))
                .isActive(true)
                .build();
        premiumHistoryMapper.createPremiumHistory(newSubscription);
        log.info("새로운 프리미엄 구독을 생성했습니다. 만료일: {}", newSubscription.getEndDate());
    }
}
