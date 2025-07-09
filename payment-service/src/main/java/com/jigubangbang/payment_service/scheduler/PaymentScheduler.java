
package com.jigubangbang.payment_service.scheduler;

import com.jigubangbang.payment_service.service.PaymentService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * 정기적인 결제 작업을 수행하는 스케줄러
 */
@Slf4j
@Component // 이 클래스를 스프링이 관리하는 빈(Bean)으로 등록합니다.
@RequiredArgsConstructor
public class PaymentScheduler {

    private final PaymentService paymentService;

    /**
     * 매일 새벽 4시에 자동 결제 로직을 실행합니다.
     * Cron Expression: 초 분 시 일 월 요일
     * "0 0 4 * * ?" -> 매일 4시 0분 0초에 실행
     */
    @Scheduled(cron = "0 0 4 * * ?")
    public void scheduleAutoPayments() {
        log.info("===== 자동 결제 스케줄러 시작 =====");
        try {
            paymentService.processScheduledPayments();
        } catch (Exception e) {
            log.error("자동 결제 스케줄러 실행 중 오류 발생", e);
        }
        log.info("===== 자동 결제 스케줄러 종료 =====");
    }
}
