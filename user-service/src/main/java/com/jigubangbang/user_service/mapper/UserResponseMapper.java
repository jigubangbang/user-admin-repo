package com.jigubangbang.user_service.mapper;

import java.util.Optional;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import com.jigubangbang.user_service.model.UserResponseDto;

@Mapper
public interface UserResponseMapper {

    /**
     * 사용자 ID로 다른 서비스에 필요한 사용자 정보만 조회합니다.
     * @param userId 조회할 사용자 ID
     * @return 사용자 정보 (Optional<UserResponseDto>)
     */
    Optional<UserResponseDto> findForPaymentService(String userId);

    /**
     * 사용자의 프리미엄 상태와 customer_uid(빌링키)를 업데이트합니다.
     * DTO의 필드가 null이 아닌 경우에만 해당 컬럼을 업데이트합니다.
     * @param requestDto 업데이트할 정보가 담긴 DTO
     * @return 영향을 받은 행의 수
     */
    int updatePremiumInfo(@Param("userId") String userId, @Param("premium") boolean premium, @Param("customerUid") String customerUid);
}
