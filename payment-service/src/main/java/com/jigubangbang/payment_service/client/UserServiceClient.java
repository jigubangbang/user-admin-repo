package com.jigubangbang.payment_service.client;

import com.jigubangbang.payment_service.model.UserResponseDto;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

/**
 * user-service와 통신하기 위한 Feign Client 인터페이스
 */
// name: Eureka에 등록된 user-service의 이름
// path: user-service의 API 공통 경로 (user-service의 @RequestMapping 값)
@FeignClient(name = "user-service", path = "/api/user")
public interface UserServiceClient {

    /**
     * user-service에 사용자 정보를 요청합니다.
     * @param userId 조회할 사용자 ID
     * @return 사용자 정보 DTO
     */
    // GET /api/user/internal/{userId} 와 같은 내부 통신용 API를 호출한다고 가정
    @GetMapping("/internal/{userId}") 
    UserResponseDto getUserInfo(@PathVariable("userId") String userId);
}
