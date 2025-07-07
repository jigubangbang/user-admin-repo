package com.jigubangbang.payment_service.client;

import com.jigubangbang.payment_service.config.PortoneConfig;
import com.jigubangbang.payment_service.model.portone.PortonePaymentResponse;
import com.jigubangbang.payment_service.model.portone.PortoneRecurringPaymentRequest;
import com.jigubangbang.payment_service.model.portone.PortoneTokenResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

@Slf4j
@Component
public class PortoneClient {

    private final WebClient webClient;
    private final PortoneConfig portoneConfig;
    private static final String PORTONE_API_BASE_URL = "https://api.iamport.kr";

    // 생성자에서 WebClient.Builder를 주입받아 webClient를 초기화합니다.
    public PortoneClient(WebClient.Builder webClientBuilder, PortoneConfig portoneConfig) {
        this.webClient = webClientBuilder.baseUrl(PORTONE_API_BASE_URL).build();
        this.portoneConfig = portoneConfig;
    }

    /**
     * 포트원 API 인증 토큰을 발급받습니다. (WebClient 방식)
     * @return 발급받은 Access Token 문자열
     */
    public String getAccessToken() {
        Map<String, String> body = new HashMap<>();
        body.put("imp_key", portoneConfig.getApiKey());
        body.put("imp_secret", portoneConfig.getApiSecret());

        try {
            PortoneTokenResponse tokenResponse = webClient.post()
                    .uri("/users/getToken")
                    .contentType(MediaType.APPLICATION_JSON)
                    .bodyValue(body)
                    .retrieve() // 요청을 보내고 응답을 받음
                    .bodyToMono(PortoneTokenResponse.class) // 응답 본문을 PortoneTokenResponse 객체로 변환
                    .block(); // 비동기 처리가 끝날 때까지 기다림 (동기처럼 사용)

            String accessToken = Objects.requireNonNull(tokenResponse).getResponse().getAccessToken();
            log.info("WebClient: 포트원 액세스 토큰 발급 성공!");
            return accessToken;

        } catch (Exception e) {
            log.error("WebClient: 포트원 액세스 토큰 발급 중 예외 발생", e);
            throw new RuntimeException("WebClient: 포트원 토큰 발급 중 오류가 발생했습니다.", e);
        }
    }

    /**
     * imp_uid를 사용하여 포트원에서 결제 정보를 조회합니다. (WebClient 방식)
     * @param impUid 조회할 포트원 결제 고유 ID
     * @param accessToken 인증을 위한 Access Token
     * @return 조회된 결제 정보 객체
     */
    public PortonePaymentResponse.PaymentInfo getPaymentInfo(String impUid, String accessToken) {
        try {
            PortonePaymentResponse paymentResponse = webClient.get()
                    .uri("/payments/" + impUid)
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken)
                    .retrieve()
                    .bodyToMono(PortonePaymentResponse.class)
                    .block();

            log.info("WebClient: 포트원 결제 정보 조회 성공: imp_uid={}", impUid);
            return Objects.requireNonNull(paymentResponse).getResponse();

        } catch (Exception e) {
            log.error("WebClient: 포트원 결제 정보 조회 중 예외 발생", e);
            throw new RuntimeException("WebClient: 결제 정보 조회 중 오류가 발생했습니다.", e);
        }
    }

    /**
     * 빌링키(customer_uid)를 사용하여 자동 결제를 요청합니다.
     * @param request 자동 결제에 필요한 정보 (빌링키, 주문번호, 금액 등)
     * @param accessToken 인증을 위한 Access Token
     * @return 포트원의 결제 처리 결과 정보
     */
    public PortonePaymentResponse.PaymentInfo requestRecurringPayment(PortoneRecurringPaymentRequest request, String accessToken) {
        log.info("자동 결제 요청 시작: merchant_uid={}", request.getMerchantUid());
        try {
            PortonePaymentResponse paymentResponse = webClient.post()
                    .uri("/subscribe/payments/again")
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken)
                    .contentType(MediaType.APPLICATION_JSON)
                    .bodyValue(request)
                    .retrieve()
                    .onStatus(status -> status.isError(), clientResponse -> 
                        clientResponse.bodyToMono(String.class).flatMap(body -> {
                            log.error("Portone API 에러 응답: status={}, body={}", clientResponse.statusCode(), body);
                            return Mono.error(new RuntimeException("Portone API 에러: " + body));
                        })
                    )
                    .bodyToMono(PortonePaymentResponse.class)
                    .doOnSuccess(response -> log.info("Portone API 성공 응답: {}", response))
                    .block();

            log.info("자동 결제 요청 응답 수신: merchant_uid={}", request.getMerchantUid());
            return Objects.requireNonNull(paymentResponse).getResponse();

        } catch (Exception e) {
            log.error("자동 결제 요청 중 예외 발생: merchant_uid={}", request.getMerchantUid(), e);
            throw new RuntimeException("자동 결제 요청 중 오류가 발생했습니다.", e);
        }
    }
}
