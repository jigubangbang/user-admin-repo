package com.jigubangbang.admin_service.mapper;

import com.jigubangbang.admin_service.model.AdminGroupDto;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.time.LocalDateTime;
import java.util.List;

@Mapper
public interface AdminGroupMapper {

    // 여행자모임 목록 조회
    List<AdminGroupDto> getMateGroups(
            @Param("nickname") String nickname,
            @Param("status") String status,
            @Param("keyword") String keyword,
            @Param("startDate") LocalDateTime startDate,
            @Param("endDate") LocalDateTime endDate
    );

    // 정보공유방 목록 조회
    List<AdminGroupDto> getInfoGroups(
            @Param("nickname") String nickname,
            @Param("status") String status,
            @Param("keyword") String keyword,
            @Param("startDate") LocalDateTime startDate,
            @Param("endDate") LocalDateTime endDate
    );

    // 여행자모임 블라인드 처리
    void blindMateGroup(@Param("groupId") int groupId);
    void blindInfoGroup(@Param("groupId") int groupId);

    // 정보공유방 블라인드 처리
    void unblindMateGroup(@Param("groupId") int groupId);
    void unblindInfoGroup(@Param("groupId") int groupId);

    // 그룹 정보 조회
    AdminGroupDto getGroupInfo(@Param("groupId") int groupId, @Param("contentType") String contentType);
}
