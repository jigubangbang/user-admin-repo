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
import com.jigubangbang.payment_service.model.PremiumStatusResponseDto;
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

    public PremiumStatusResponseDto getLatestPremiumStatusForUser(String userId) {
        // 1. 사용자 정보에서 customerUid 가져오기
        UserResponseDto userInfo = userServiceClient.getUserInfo(userId);
        String customerUid = userInfo != null ? userInfo.getCustomerUid() : null;

        // 2. 프리미엄 구독 이력 가져오기
        PremiumHistoryDto premiumHistory = premiumHistoryMapper.findLatestByUserId(userId)
                .orElseGet(() -> {
                    // 구독 기록이 없는 경우, 기본값으로 isActive: false를 가진 DTO 반환
                    return PremiumHistoryDto.builder()
                            .userId(userId)
                            .isActive(false)
                            .build();
                });
        
        // 3. PremiumStatusResponseDto에 담아 반환
        return PremiumStatusResponseDto.builder()
                .premiumHistory(premiumHistory)
                .customerUid(customerUid)
                .build();
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

    /**
     * 사용자의 결제 수단을 변경합니다.
     * 0원 결제를 통해 카드 정보를 인증하고, 새로운 빌링키(customer_uid)로 업데이트합니다.
     * @param userId 결제 수단을 변경할 사용자 ID
     * @param impUid 프론트엔드에서 0원 결제 후 받은 Iamport 결제 고유 ID
     */
    @Transactional
    public void changePaymentMethod(String userId, String impUid) {
        log.info("사용자 [{}]의 결제 수단 변경(결제 후 즉시 취소 방식) 요청 수신. imp_uid: {}", userId, impUid);

        try {
            // 1. Portone API를 통해 990원 결제 정보 조회 및 검증
            String accessToken = portoneClient.getAccessToken();
            PortonePaymentResponse.PaymentInfo paymentInfo = portoneClient.getPaymentInfo(impUid, accessToken);

            // 2. 100원 결제가 맞는지, 상태가 "paid"가 맞는지 검증
            if (paymentInfo.getAmount().compareTo(BigDecimal.valueOf(100)) != 0) {
                throw new IllegalStateException("결제 금액이 100원이 아닙니다. (amount=" + paymentInfo.getAmount() + ")");
            }
            if (!"paid".equalsIgnoreCase(paymentInfo.getStatus())) {
                throw new IllegalStateException("결제 상태가 'paid'가 아닙니다. (status=" + paymentInfo.getStatus() + ")");
            }

            // 3. 새로운 빌링키(customer_uid) 및 카드 정보 추출
            String newCustomerUid = paymentInfo.getCustomerUid();
            if (newCustomerUid == null || newCustomerUid.isBlank()) {
                throw new IllegalStateException("새로운 빌링키(customer_uid)를 받아오지 못했습니다.");
            }
            log.info("새로운 빌링키 및 카드 정보 획득 성공: customer_uid={}, card_number={}", newCustomerUid, paymentInfo.getCardNumber());

            // 4. user-service에 빌링키 업데이트 요청
            UserPremiumUpdateRequestDto userUpdateRequest = UserPremiumUpdateRequestDto.builder()
                    .customerUid(newCustomerUid)
                    .build();
            userServiceClient.updateUserPremiumStatus(userId, userUpdateRequest);
            log.info("사용자 [{}]의 빌링키를 성공적으로 업데이트했습니다.", userId);

            // 5. payment 테이블에 카드 정보 업데이트 내역 기록
            PaymentHistoryDto cardUpdateRecord = PaymentHistoryDto.builder()
                    .userId(userId)
                    .impUid(impUid)
                    .merchantUid(paymentInfo.getMerchantUid())
                    .amount(paymentInfo.getAmount().intValue())
                    .payStatus("CARD_UPDATED") // 카드 변경 기록 상태
                    .payMethod(paymentInfo.getPayMethod())
                    .cardName(paymentInfo.getCardName())
                    .cardNumberMasked(paymentInfo.getCardNumber())
                    .build();
            paymentHistoryMapper.createPaymentHistory(cardUpdateRecord);
            log.info("payment 테이블에 결제 수단 변경 내역 기록 완료: userId={}, impUid={}", userId, impUid);

            // 6. 방금 결제된 990원 즉시 환불 처리
            try {
                log.info("카드 등록을 위한 결제(imp_uid={})의 즉시 환불을 시작합니다.", impUid);
                Map<String, String> refundPayload = new HashMap<>();
                refundPayload.put("imp_uid", impUid);
                portoneClient.requestRefund(refundPayload, accessToken);
                log.info("카드 등록 결제 즉시 환불 성공: imp_uid={}", impUid);
            } catch (Exception refundEx) {
                // 중요: 환불 실패 시 심각한 에러 로그를 남겨서 수동 처리가 가능하도록 함
                log.error("!!!!!!!!!! [심각] 카드 등록 결제 즉시 환불 실패 !!!!!!!!!! imp_uid={}, userId={}. 수동 환불 처리가 필요합니다.", impUid, userId, refundEx);
                // 환불이 실패하더라도, 카드 등록 자체는 성공했으므로 여기서 예외를 다시 던지지는 않음.
            }

        } catch (Exception e) {
            log.error("결제 수단 변경 처리 중 오류 발생: userId={}", userId, e);
            // 카드 ���록 과정 자체에서 오류 발생 시, 결제가 되었을 수 있으므로 환불 시도
            try {
                log.warn("결제 수단 변경 프로세스 실패로 인한 자동 환불 시도: imp_uid={}", impUid);
                String accessToken = portoneClient.getAccessToken();
                Map<String, String> refundPayload = new HashMap<>();
                refundPayload.put("imp_uid", impUid);
                portoneClient.requestRefund(refundPayload, accessToken);
                log.info("프로세스 실패 후 자동 환불 성공: imp_uid={}", impUid);
            } catch (Exception refundEx) {
                log.error("!!!!!!!!!! [심각] 프로세스 실패 후 자동 환불도 실패 !!!!!!!!!! imp_uid={}. 수동 환불 처리가 필요합니다.", impUid, refundEx);
            }
            throw new RuntimeException("결제 수단 변경 처리 중 오류가 발생했습니다.", e);
        }
    }

    @Transactional
    public Map<String, Object> processWebhook(String impUid, String merchantUid, String status) {
        log.info("웹훅 처리 시작: imp_uid={}, merchant_uid={}, status={}", impUid, merchantUid, status);

        // 결제 수단 변경을 위한 0원 결제 웹훅은 무시
        if (merchantUid != null && merchantUid.startsWith("mid_")) {
            log.info("결제 수단 변경 웹훅 (merchant_uid: {})이므로 처리하지 않습니다.", merchantUid);
            return null;
        }

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
                order.setPayMethod(paymentInfo.getPayMethod()); // 결제 수단 설정
                order.setCardName(paymentInfo.getCardName()); // 카드사 설정
                order.setCardNumberMasked(paymentInfo.getCardNumber()); // 마스킹된 카드 번호 설정
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
                    .isPremium(true)
                    .customerUid(customerUid)
                    .build();
            userServiceClient.updateUserPremiumStatus(userId, userUpdateRequest);
            log.info("user-service에 사용자 [{}] 프리미엄 상태 및 빌링키 업데이트 완료", userId);
        } catch (Exception e) {
            log.error("user-service 호출 중 오류 발생", e);
        }
    }

    public List<PaymentHistoryDto> getPaymentHistoryByUserId(String userId) {
        log.info("사용자 [{}]의 결제 내역 조회를 요청합니다.", userId);
        return paymentHistoryMapper.findByUserId(userId);
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

    /**
     * 사용자의 환불 요청을 처리합니다.
     * @param userId 요청한 사용자 ID
     * @param merchantUid 환불할 주문 ID
     */
    @Transactional
    public void requestRefund(String userId, String merchantUid) {
        log.info("사용자 [{}]의 환불 요청 수신. 주문 ID: {}", userId, merchantUid);

        // 1. 환불할 결제 내역 조회 및 유효성 검증
        PaymentHistoryDto paymentToRefund = paymentHistoryMapper.findByMerchantUid(merchantUid)
                .orElseThrow(() -> new IllegalStateException("존재하지 않는 주문 번호입니다: " + merchantUid));

        if (!paymentToRefund.getUserId().equals(userId)) {
            throw new SecurityException("환불을 요청할 권한이 없습니다.");
        }

        if (!"PAID".equals(paymentToRefund.getPayStatus())) {
            throw new IllegalStateException("이미 환불되었거나 결제가 완료되지 않은 건입니다.");
        }

        try {
            // 2. Portone(Iamport) 환불 API 호출
            String accessToken = portoneClient.getAccessToken();
            Map<String, String> refundPayload = new HashMap<>();
            refundPayload.put("imp_uid", paymentToRefund.getImpUid());
            // 필요 시, 부분 환불을 위한 amount, reason 등을 추가할 수 있습니다.
            
            portoneClient.requestRefund(refundPayload, accessToken);
            log.info("Portone 환불 API 호출 성공: imp_uid={}", paymentToRefund.getImpUid());

            // 3. DB 상태 업데이트: payment_history 상태 변경
            paymentToRefund.setPayStatus("CANCELLED");
            paymentToRefund.setCancelledAt(LocalDateTime.now());
            paymentHistoryMapper.updatePaymentStatus(paymentToRefund);
            log.info("payment_history 테이블 업데이트 완료: status=CANCELLED");

            // 4. DB 상태 업데이트: premium_log 비활성화
            // 가장 최근의 활성 구독을 찾아 비활성화합니다.
            premiumHistoryMapper.findActiveByUserId(userId).ifPresent(activeSubscription -> {
                activeSubscription.setIsActive(false);
                activeSubscription.setEndDate(LocalDateTime.now()); // 환불 시 즉시 종료
                premiumHistoryMapper.updatePremiumHistory(activeSubscription);
                log.info("premium_log 테이블 업데이트 완료: id={}, isActive=false", activeSubscription.getId());
            });

            // 5. DB 상태 업데이트: user-service에 프리미엄 상태 해제 및 빌링키 제거 요청
            UserPremiumUpdateRequestDto userUpdateRequest = UserPremiumUpdateRequestDto.builder()
                    .isPremium(false)
                    .customerUid(null) // 빌링키도 함께 제거
                    .build();
            userServiceClient.updateUserPremiumStatus(userId, userUpdateRequest);
            log.info("user-service에 프리미엄 해제 및 빌링키 제거 요청 완료");

        } catch (Exception e) {
            log.error("환불 처리 중 오류 발생: userId={}, merchantUid={}", userId, merchantUid, e);
            throw new RuntimeException("환불 처리 중 오류가 발생했습니다. " + e.getMessage(), e);
        }
    }
}
