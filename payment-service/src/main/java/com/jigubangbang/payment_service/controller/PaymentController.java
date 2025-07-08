package com.jigubangbang.payment_service.controller;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.jigubangbang.payment_service.model.PaymentMethodChangeRequestDto;
import com.jigubangbang.payment_service.model.PremiumStatusResponseDto;
import com.jigubangbang.payment_service.model.PaymentHistoryDto;
import com.jigubangbang.payment_service.model.PremiumHistoryDto;
import com.jigubangbang.payment_service.model.RefundRequestDto;
import com.jigubangbang.payment_service.service.PaymentService;

import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@RestController
@RequestMapping("/payment")
@RequiredArgsConstructor
public class PaymentController {

    private final PaymentService paymentService;

    @PostMapping("/premium/subscribe")
    public ResponseEntity<Map<String, Object>> subscribePremium(@RequestHeader("User-Id") String userId) {
        final int PREMIUM_AMOUNT = 990; // 프리미엄 구독료 고정

        try {
            // 서비스 레이어를 호출하여 결제 준비
            String merchantUid = paymentService.prepareNewSubscription(userId, PREMIUM_AMOUNT);

            // 프론트엔드에 전달할 정보 구성
            Map<String, Object> response = new HashMap<>();
            response.put("merchant_uid", merchantUid);
            response.put("amount", PREMIUM_AMOUNT);
            response.put("userId", userId); // 사용자 ID 추가

            log.info("프리미엄 구독 신청 처리 완료: userId={}, merchantUid={}", userId, merchantUid);
            return ResponseEntity.ok(response);

        } catch (Exception e) {
            log.error("프리미엄 구독 처리 중 오류 발생: userId={}", userId, e);
            return ResponseEntity.internalServerError().body(Map.of("error", "결제 준비 중 오류가 발생했습니다."));
        }
    }

    @PostMapping("/premium/change-method")
    public ResponseEntity<Void> changePaymentMethod(
            @RequestHeader("User-Id") String userId,
            @RequestBody PaymentMethodChangeRequestDto requestDto) {
        try {
            paymentService.changePaymentMethod(userId, requestDto.getImpUid());
            log.info("결제 수단 변경 성공: userId={}, impUid={}", userId, requestDto.getImpUid());
            return ResponseEntity.ok().build();
        } catch (Exception e) {
            log.error("결제 수단 변경 처리 중 오류 발생: userId={}", userId, e);
            return ResponseEntity.internalServerError().build();
        }
    }

    @GetMapping("/premium/status")
    public ResponseEntity<PremiumStatusResponseDto> getPremiumStatus(@RequestHeader("User-Id") String userId) {
        try {
            PremiumStatusResponseDto subscriptionStatus = paymentService.getLatestPremiumStatusForUser(userId);
            log.info("구독 상태 조회 완료: userId={}, isActive={}", userId, subscriptionStatus.getPremiumHistory().getIsActive());
            return ResponseEntity.ok(subscriptionStatus);
        } catch (Exception e) {
            log.error("구독 상태 조회 중 오류 발생: userId={}", userId, e);
            // 오류 발생 시에도 빈 DTO를 반환하여 프론트엔드에서 null 체크를 피하도록 함
            return ResponseEntity.internalServerError().body(new PremiumStatusResponseDto());
        }
    }

    @DeleteMapping("/premium/cancel")
    public ResponseEntity<Void> cancelPremiumSubscription(@RequestHeader("User-Id") String userId) {
        try {
            paymentService.requestSubscriptionCancellation(userId);
            log.info("프리미엄 구독 해지 요청 처리 완료: userId={}", userId);
            return ResponseEntity.ok().build();
        } catch (Exception e) {
            log.error("구독 해지 요청 처리 중 오류 발생: userId={}", userId, e);
            return ResponseEntity.internalServerError().build();
        }
    }

    @PostMapping("/webhook/iamport")
    public ResponseEntity<Void> portoneWebhook(@RequestBody PortoneWebhookPayload payload) {
        log.info("포트원 웹훅 수신: {}", payload);
        try {
            // 1. 서비스의 트랜잭션 메소드 호출
            Map<String, Object> result = paymentService.processWebhook(payload.getImp_uid(), payload.getMerchant_uid(), payload.getStatus());

            // 2. 트랜잭션이 끝난 후, 별도의 외부 서비스 호출 실행
            if (result != null) {
                PaymentHistoryDto payment = (PaymentHistoryDto) result.get("payment");
                String customerUid = (String) result.get("customerUid");

                if (payment != null && payment.getUserId() != null && customerUid != null) {
                    try {
                        // 트랜잭션이 완전히 분리된 상태에서 user-service 호출
                        paymentService.updateUserPremiumExternal(payment.getUserId(), customerUid);
                        log.info("프리미엄 상태 및 빌링키 외부 업데이트 성공: userId={}", payment.getUserId());
                    } catch (Exception ex) {
                        log.error("user-service 호출 중 오류 발생 (트랜잭션 외부)", ex);
                        // 여기서의 실패는 웹훅의 성공 응답에 영향을 주지 않도록 처리 (예: 재시도 큐에 적재)
                    }
                }
            }

            return ResponseEntity.ok().build();
        } catch (Exception e) {
            log.error("웹훅 처리 중 오류 발생: imp_uid={}", payload.getImp_uid(), e);
            return ResponseEntity.badRequest().build();
        }
    }

    @GetMapping("/history")
    public ResponseEntity<List<PaymentHistoryDto>> getPaymentHistory(@RequestHeader("User-Id") String userId) {
        try {
            List<PaymentHistoryDto> history = paymentService.getPaymentHistoryByUserId(userId);
            if (history.isEmpty()) {
                return ResponseEntity.notFound().build();
            }
            return ResponseEntity.ok(history);
        } catch (Exception e) {
            log.error("결제 내역 조회 중 오류 발생: userId={}", userId, e);
            return ResponseEntity.internalServerError().build();
        }
    }

    @PostMapping("/internal/auto-payment")
    public ResponseEntity<String> triggerAutoPayment() {
        try {
            log.info("수동 자동 결제 실행 요청 수신");
            paymentService.processScheduledPayments();
            return ResponseEntity.ok("자동 결제 로직을 성공적으로 실행했습니다.");
        } catch (Exception e) {
            log.error("수동 자동 결제 실행 중 오류 발생", e);
            return ResponseEntity.internalServerError().body("자동 결제 실행 중 오류가 발생했습니다.");
        }
    }

    @Data
    static class PortoneWebhookPayload {
        private String imp_uid;
        private String merchant_uid;
        private String status;
        private String pay_method; // 결제 수단 추가
        private String card_name; // 카드사 추가
        private String card_number; // 마스킹된 카드 번호 추가
    }

    @PostMapping("/refund/request")
    public ResponseEntity<Map<String, String>> refundRequest(
            @RequestHeader("User-Id") String userId,
            @RequestBody RefundRequestDto requestDto) {
        try {
            paymentService.requestRefund(userId, requestDto.getMerchantUid());
            log.info("환불 요청 성공: userId={}, merchantUid={}", userId, requestDto.getMerchantUid());
            return ResponseEntity.ok(Map.of("message", "환불 요청이 정상적으로 처리되었습니다."));
        } catch (Exception e) {
            log.error("환불 요청 처리 중 오류 발생: userId={}, merchantUid={}", userId, requestDto.getMerchantUid(), e);
            return ResponseEntity.internalServerError().body(Map.of("error", e.getMessage()));
        }
    }
}
