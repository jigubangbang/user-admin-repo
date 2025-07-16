package com.jigubangbang.admin_service.service;

import com.jigubangbang.admin_service.mapper.AdminGroupMapper;
import com.jigubangbang.admin_service.mapper.BlindCountMapper;
import com.jigubangbang.admin_service.model.AdminGroupDto;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

@Service
@RequiredArgsConstructor
public class AdminGroupService {

    private final AdminGroupMapper adminGroupMapper;
    private final BlindCountMapper blindCountMapper;

    // 그룹 목록 조회
    public List<AdminGroupDto> getAllGroups(String contentType, String nickname, String status, String keyword,
            LocalDateTime startDate, LocalDateTime endDate) {
        List<AdminGroupDto> result = new ArrayList<>();

        if (contentType == null || contentType.equals("all")) {
            result.addAll(adminGroupMapper.getMateGroups(nickname, status, keyword, startDate, endDate));
            result.addAll(adminGroupMapper.getInfoGroups(nickname, status, keyword, startDate, endDate));
        } else if (contentType.equals("mate")) {
            result.addAll(adminGroupMapper.getMateGroups(nickname, status, keyword, startDate, endDate));
        } else if (contentType.equals("info")) {
            result.addAll(adminGroupMapper.getInfoGroups(nickname, status, keyword, startDate, endDate));
        } else {
            throw new IllegalArgumentException("Invalid contentType: " + contentType);
        }

        result.sort(Comparator.comparing(AdminGroupDto::getCreatedAt).reversed());
        return result;
    }

    // 그룹 블라인드 처리
    @Transactional  
    public void blindGroup(int groupId, String contentType) {
        AdminGroupDto groupInfo = adminGroupMapper.getGroupInfo(groupId, contentType);
        
        if ("BLINDED".equals(groupInfo.getStatus())) { // 이미 블라인드된 상태면 처리하지 않음 
            return;
        }
        
        switch (contentType) {
            case "mate" -> adminGroupMapper.blindMateGroup(groupId);
            case "info" -> adminGroupMapper.blindInfoGroup(groupId);
            default -> throw new IllegalArgumentException("Invalid contentType: " + contentType);
        }
        
        blindCountMapper.increaseBlindCount(groupInfo.getUserId()); // blind_count + 1 (추가)
    }

    // 그룹 블라인드 해제
    @Transactional  
    public void unblindGroup(int groupId, String contentType) {
        AdminGroupDto groupInfo = adminGroupMapper.getGroupInfo(groupId, contentType);
        
        if ("VISIBLE".equals(groupInfo.getStatus())) { // 이미 공개된 상태면 처리하지 않음
            return;
        }
        
        switch (contentType) {
            case "mate" -> adminGroupMapper.unblindMateGroup(groupId);
            case "info" -> adminGroupMapper.unblindInfoGroup(groupId);
            default -> throw new IllegalArgumentException("Invalid contentType: " + contentType);
        }
        
        blindCountMapper.decreaseBlindCount(groupInfo.getUserId()); // blind_count - 1 
    }

}
