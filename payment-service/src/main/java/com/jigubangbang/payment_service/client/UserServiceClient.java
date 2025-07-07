package com.jigubangbang.payment_service.client;

import com.jigubangbang.payment_service.model.UserPremiumUpdateRequestDto;
import com.jigubangbang.payment_service.model.UserResponseDto;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;

/**
 * user-service와 통신하기 위한 Feign Client 인터페이스
 */
@FeignClient(name = "user-service")
public interface UserServiceClient {

    /**
     * user-service에 사용자 정보를 요청합니다.
     * @param userId 조회할 사용자 ID
     * @return 사용자 정보 DTO
     */
    // =====👇 user-service의 컨트롤러 경로인 /user를 반드시 포함해야 합니다. =====
    @GetMapping("/user/internal/{userId}")
    UserResponseDto getUserInfo(@PathVariable("userId") String userId);

    // 최초 결제 성공 후, is_premium과 customer_uid 업데이트를 위해 호출
    @PutMapping("/user/internal/premium/{userId}")
    void updateUserPremiumStatus(
            @PathVariable("userId") String userId,
            @RequestBody UserPremiumUpdateRequestDto request
    );
}