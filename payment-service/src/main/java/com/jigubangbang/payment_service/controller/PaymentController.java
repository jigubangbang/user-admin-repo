package com.jigubangbang.payment_service.controller;

import com.jigubangbang.payment_service.model.PaymentHistoryDto;
import com.jigubangbang.payment_service.service.PaymentService;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@Slf4j
@RestController
// =====👇 API 게이트웨이의 StripPrefix 규칙에 맞춰 경로를 수정합니다. =====
@RequestMapping("/payment")
// ====================================================================
@RequiredArgsConstructor
public class PaymentController {

    private final PaymentService paymentService;

    /**
     * 결제 정보를 사전에 등록하고, 결제에 필요한 merchant_uid를 반환합니다.
     */
    @PostMapping("/prepare")
    public ResponseEntity<Map<String, String>> preparePayment(@RequestBody PaymentHistoryDto request) {
        String merchantUid = paymentService.preparePayment(request);
        return ResponseEntity.ok(Map.of("merchant_uid", merchantUid));
    }

    /**
     * 포트원 결제 웹훅(Webhook) 수신 엔드포인트
     */
    @PostMapping("/webhook/iamport")
    public ResponseEntity<Void> portoneWebhook(@RequestBody PortoneWebhookPayload payload) {
        log.info("포트원 웹훅 수신: {}", payload);
        try {
            paymentService.processWebhook(payload.getImp_uid(), payload.getMerchant_uid());
            return ResponseEntity.ok().build();
        } catch (Exception e) {
            log.error("웹훅 처리 중 오류 발생: imp_uid={}", payload.getImp_uid(), e);
            return ResponseEntity.badRequest().build();
        }
    }

    /**
     * 자동 결제 로직을 수동으로 실행하기 위한 엔드포인트 (내부 테스트용)
     */
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

    /**
     * 포트원 웹훅 요청의 Body를 받기 위한 DTO 클래스
     */
    @Data
    static class PortoneWebhookPayload {
        private String imp_uid;
        private String merchant_uid;
        private String status;
    }
}
