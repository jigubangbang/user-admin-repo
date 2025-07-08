package com.jigubangbang.user_service.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.jigubangbang.user_service.mapper.UserResponseMapper;
import com.jigubangbang.user_service.model.UserPremiumUpdateRequestDto;
import com.jigubangbang.user_service.model.UserResponseDto;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
@Slf4j
@Service
@RequiredArgsConstructor
public class UserResponseService {

    private final UserResponseMapper userResponseMapper;

    /**
     * 다른 서비스(payment-service)의 요청에 따라 사용자 정보를 조회하는 메소드
     * @param userId 조회할 사용자 ID
     * @return UserResponseDto (customer_uid 포함)
     */
    public UserResponseDto getUserInfoForInternal(String userId) {
        // Mapper를 사용하여 필요한 데이터만 담은 DTO를 직접 조회하여 반환합니다.
        return userResponseMapper.findForPaymentService(userId)
                .orElseThrow(() -> new RuntimeException("사용자를 찾을 수 없습니다: " + userId));
    }

    /**
     * 사용자의 프리미엄 정보를 업데이트하는 서비스 메소드
     */
    @Transactional
    public void updateUserPremium(String userId, UserPremiumUpdateRequestDto requestDto) {
        // isPremium과 customerUid를 모두 업데이트
        int updatedRows = userResponseMapper.updatePremiumInfo(
            userId,
            requestDto.getIsPremium(),
            requestDto.getCustomerUid()
        );

        if (updatedRows == 0) {
            throw new RuntimeException("프리미엄 정보 업데이트에 실패했습니다. 사용자 ID: " + userId);
        }
        log.info("사용자 [{}]의 프리미엄 정보 업데이트 성공", userId);
    }
}
