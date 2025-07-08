package com.jigubangbang.user_service.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.jigubangbang.user_service.model.UserPremiumUpdateRequestDto;
import com.jigubangbang.user_service.model.UserResponseDto;
import com.jigubangbang.user_service.service.UserResponseService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@RestController
// API 게이트웨이의 StripPrefix=1 규칙에 따라 /api가 제거된 경로를 사용합니다.
@RequestMapping("/user")
@RequiredArgsConstructor
public class UserResponseController {

    private final UserResponseService userResponseService;

    /**
     * 서비스 간 내부 통신을 위한 사용자 정보 조회 API
     * payment-service가 자동 결제 시 빌링키를 조회하기 위해 호출합니다.
     * @param userId 조회할 사용자 ID
     * @return 사용자 정보 (customer_uid 포함)
     */
    @GetMapping("/internal/{userId}")
    public ResponseEntity<UserResponseDto> getUserInfoForPayment(@PathVariable String userId) {
        log.info("내부 통신: 사용자 [{}] 정보 조회 요청 수신", userId);
        try {
            UserResponseDto userInfo = userResponseService.getUserInfoForInternal(userId);
            return ResponseEntity.ok(userInfo);
        } catch (Exception e) {
            log.error("사용자 정보 조회 중 오류 발생: userId={}", userId, e);
            return ResponseEntity.notFound().build();
        }
    }

    /**
     * payment-service로부터 사용자 프리미엄 정보 업데이트 요청을 받는 API
     */
    @PutMapping("/internal/premium/{userId}")
    public ResponseEntity<Void> updateUserPremiumStatus(
            @PathVariable String userId,
            @RequestBody UserPremiumUpdateRequestDto request) {
        log.info("내부 통신: 사용자 [{}] 프리미엄 상태 업데이트 요청 수신. customer_uid: {}", userId, request.getCustomerUid());
        try {
            userResponseService.updateUserPremium(userId, request);
            return ResponseEntity.ok().build();
        } catch (Exception e) {
            log.error("프리미엄 상태 업데이트 중 오류 발생: userId={}", userId, e);
            return ResponseEntity.internalServerError().build();
        }
    }
}
