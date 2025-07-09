package com.jigubangbang.admin_service.controller;

import com.jigubangbang.admin_service.model.AdminGroupDto;
import com.jigubangbang.admin_service.service.AdminGroupService;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;

@RestController
@RequestMapping("/admin/groups")
@RequiredArgsConstructor
public class AdminGroupController {

    private final AdminGroupService adminGroupService;

    // 그룹 목록 조회
    @GetMapping
    public ResponseEntity<List<AdminGroupDto>> getAllGroups(
            @RequestParam(required = false) String contentType,  // "mate" | "info" | "all"
            @RequestParam(required = false) String nickname,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime endDate
    ) {
        List<AdminGroupDto> groups = adminGroupService.getAllGroups(contentType, nickname, status, keyword, startDate, endDate);
        return ResponseEntity.ok(groups);
    }

    // 블라인드 처리
    @PutMapping("/{groupId}/blind")
    public ResponseEntity<Void> blindGroup(
            @PathVariable int groupId,
            @RequestParam String contentType
    ) {
        adminGroupService.blindGroup(groupId, contentType);
        return ResponseEntity.ok().build();
    }

    // 블라인드 해제
    @PutMapping("/{groupId}/unblind")
    public ResponseEntity<Void> unblindGroup(
            @PathVariable int groupId,
            @RequestParam String contentType
    ) {
        adminGroupService.unblindGroup(groupId, contentType);
        return ResponseEntity.ok().build();
    }
    
}
