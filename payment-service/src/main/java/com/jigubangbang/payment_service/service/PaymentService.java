package com.jigubangbang.payment_service.service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.jigubangbang.payment_service.client.PortoneClient;
import com.jigubangbang.payment_service.client.UserServiceClient;
import com.jigubangbang.payment_service.mapper.PaymentHistoryMapper;
import com.jigubangbang.payment_service.mapper.PremiumHistoryMapper;
import com.jigubangbang.payment_service.model.PaymentHistoryDto;
import com.jigubangbang.payment_service.model.PremiumHistoryDto;
import com.jigubangbang.payment_service.model.UserPremiumUpdateRequestDto;
import com.jigubangbang.payment_service.model.UserResponseDto;
import com.jigubangbang.payment_service.model.portone.PortonePaymentResponse;
import com.jigubangbang.payment_service.model.portone.PortoneRecurringPaymentRequest;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j
@RequiredArgsConstructor
public class PaymentService {

    private final PaymentHistoryMapper paymentHistoryMapper;
    private final PremiumHistoryMapper premiumHistoryMapper;
    private final PortoneClient portoneClient;
    private final UserServiceClient userServiceClient;

    @Value("${portone.webhook-url}")
    private String portoneWebhookUrl;

    @Transactional
    public String prepareNewSubscription(String userId, int amount) {
        String merchantUid = "order_" + UUID.randomUUID().toString().replace("-", "");
        PaymentHistoryDto newPayment = PaymentHistoryDto.builder()
                .userId(userId)
                .merchantUid(merchantUid)
                .amount(amount)
                .payStatus("READY")
                .build();
        paymentHistoryMapper.createPaymentHistory(newPayment);
        log.info("결제 정보 사전 등록 완료: userId={}, merchant_uid={}", userId, merchantUid);
        return merchantUid;
    }

    

    public Optional<PremiumHistoryDto> getActiveSubscription(String userId) {
        return premiumHistoryMapper.findActiveByUserId(userId);
    }

    public PremiumHistoryDto getLatestPremiumStatusForUser(String userId) {
        return premiumHistoryMapper.findLatestByUserId(userId)
                .orElseGet(() -> {
                    // 구독 기록이 없는 경우, 기본값으로 isActive: false를 가진 DTO 반환
                    return PremiumHistoryDto.builder()
                            .userId(userId)
                            .isActive(false)
                            .build();
                });
    }

    /**
     * 구독 해지를 요청합니다. (다음 결제일에 갱신되지 않도록 처리)
     * 실제 데이터 비활성화는 스케줄러가 처리합니다.
     * @param userId 해지를 요청한 사용자 ID
     */
    @Transactional
    public void requestSubscriptionCancellation(String userId) {
        log.info("사용자 [{}]의 구독 해지 요청 수신. 빌링키를 제거하고 premium_log를 비활성화합니다.", userId);
        try {
            // 1. user-service에 빌링키(customer_uid)만 null로 업데이트 요청
            //    is_premium 상태는 유지하여, 만료일까지 혜택을 보장합니다.
            UserPremiumUpdateRequestDto userUpdateRequest = UserPremiumUpdateRequestDto.builder()
                    .customerUid(null) // 빌링키만 null로 설정
                    .build();
            userServiceClient.updateUserPremiumStatus(userId, userUpdateRequest);
            log.info("사용자 [{}]의 빌링키 제거 완료. 다음 자동 결제가 중단됩니다.", userId);

            // 2. premium_log에서 현재 활성 구독을 찾아 is_active를 false로 업데이트
            premiumHistoryMapper.findActiveByUserId(userId).ifPresent(activeSubscription -> {
                activeSubscription.setIsActive(false);
                // end_date는 변경하지 않아, 남은 기간 동안 혜택이 유지됨을 나타냄
                premiumHistoryMapper.updatePremiumHistory(activeSubscription);
                log.info("premium_log의 활성 구독(id={})을 비활성화 처리했습니다.", activeSubscription.getId());
            });

        } catch (Exception e) {
            log.error("구독 해지 처리 중 오류 발생: userId={}", userId, e);
            throw new RuntimeException("구독 해지 처리 중 오류가 발생했습니다.", e);
        }
    }

    @Transactional
    public Map<String, Object> processWebhook(String impUid, String merchantUid, String status) {
        log.info("웹훅 처리 시작: imp_uid={}, merchant_uid={}, status={}", impUid, merchantUid, status);
        try {
            PaymentHistoryDto order = paymentHistoryMapper.findByMerchantUid(merchantUid)
                    .orElseThrow(() -> new RuntimeException("존재하지 않는 주문입니다. merchant_uid=" + merchantUid));

            if ("paid".equals(status)) {
                log.info("유효한 결제 확인. 결제 상태: {}", status);

                String accessToken = portoneClient.getAccessToken();
                PortonePaymentResponse.PaymentInfo paymentInfo = portoneClient.getPaymentInfo(impUid, accessToken);
                if (paymentInfo.getAmount().compareTo(new BigDecimal(order.getAmount())) != 0) {
                    throw new RuntimeException("결제 금액이 일치하지 않습니다.");
                }

                order.setPayStatus("PAID");
                order.setImpUid(impUid);
                paymentHistoryMapper.updatePaymentStatus(order);

                updatePremiumSubscription(order.getUserId());

                String customerUid = paymentInfo.getCustomerUid();
                log.info("Portone에서 받은 customer_uid: {}", customerUid);

                log.info("결제 및 구독 처리 완료. userId={}, 트랜잭션 외부에서 프리미엄 상태를 업데이트하세요", order.getUserId());
                Map<String, Object> result = new HashMap<>();
                result.put("payment", order);
                result.put("customerUid", customerUid);
                return result;
            } else {
                log.warn("결제가 완료되지 않았습니다. 상태: {}", status);
                // 실패 또는 다른 상태 처리 (예: 결제 실패 기록)
                order.setPayStatus(status.toUpperCase()); // 예: FAILED
                order.setImpUid(impUid);
                paymentHistoryMapper.updatePaymentStatus(order);
                return null;
            }
        } catch (Exception e) {
            log.error("웹훅 처리 중 심각한 오류 발생", e);
            throw new RuntimeException("웹훅 처리 중 오류가 발생했습니다.", e);
        }
    }

    public void updateUserPremiumExternal(String userId, String customerUid) {
        try {
            UserPremiumUpdateRequestDto userUpdateRequest = UserPremiumUpdateRequestDto.builder()
                    .premium(true)
                    .customerUid(customerUid)
                    .build();
            userServiceClient.updateUserPremiumStatus(userId, userUpdateRequest);
            log.info("user-service에 사용자 [{}] 프리미엄 상태 및 빌링키 업데이트 완료", userId);
        } catch (Exception e) {
            log.error("user-service 호출 중 오류 발생", e);
        }
    }

    @Transactional
    public void processScheduledPayments() {
        log.info("===== 자동 결제 스케줄러 실행 시작 =====");

        LocalDateTime tomorrow = LocalDateTime.now().plusDays(1);
        List<PremiumHistoryDto> targets = premiumHistoryMapper.findExpiringSubscriptions(tomorrow);

        if (targets.isEmpty()) {
            log.info("자동 결제 대상이 없습니다.");
            log.info("===== 자동 결제 스케줄러 실행 종료 =====");
            return;
        }

        log.info("자동 결제 대상 {}건 발견", targets.size());
        String accessToken = portoneClient.getAccessToken();

        for (PremiumHistoryDto subscription : targets) {
            String userId = subscription.getUserId();
            log.info("사용자 [{}]에 대한 자동 결제를 시도합니다.", userId);

            try {
                UserResponseDto userInfo = userServiceClient.getUserInfo(userId);
                String billingKey = userInfo.getCustomerUid();

                if (billingKey == null || billingKey.isBlank()) {
                    log.warn("사용자 [{}]의 빌링키(customer_uid)가 존재하지 않아 자동 결제를 건너뜁니다.", userId);
                    continue;
                }

                BigDecimal amountToPay = new BigDecimal(990);
                String productName = "월간 프리미엄 구독 자동 연장";

                PaymentHistoryDto autoPaymentDto = PaymentHistoryDto.builder()
                        .userId(userId)
                        .amount(amountToPay.intValue())
                        .build();
                String newMerchantUid = this.prepareNewSubscription(userId, amountToPay.intValue());

                PortoneRecurringPaymentRequest recurringRequest = PortoneRecurringPaymentRequest.builder()
                        .customerUid(billingKey)
                        .merchantUid(newMerchantUid)
                        .amount(amountToPay)
                        .name(productName)
                        .noticeUrl(portoneWebhookUrl)
                        .build();

                portoneClient.requestRecurringPayment(recurringRequest, accessToken);

            } catch (Exception e) {
                log.error("사용자 [{}]의 자동 결제 처리 중 오류 발생", userId, e);
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
