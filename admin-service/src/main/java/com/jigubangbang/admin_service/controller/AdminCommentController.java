package com.jigubangbang.admin_service.controller;

import com.jigubangbang.admin_service.model.AdminCommentDto;
import com.jigubangbang.admin_service.service.AdminCommentService;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;

@RestController
@RequestMapping("/admin/comments")
@RequiredArgsConstructor
public class AdminCommentController {

    private final AdminCommentService adminCommentService;

    // 댓글 목록 조회
    @GetMapping
    public ResponseEntity<List<AdminCommentDto>> getAllComments(
            @RequestParam(required = false) String contentType, // community | feed | group | all
            @RequestParam(required = false) String nickname,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime endDate
    ) {
        List<AdminCommentDto> comments = adminCommentService.getAllComments(
                contentType, nickname, status, keyword, startDate, endDate
        );
        return ResponseEntity.ok(comments);
    }

    // 블라인드 처리
    @PutMapping("/{commentId}/blind")
    public ResponseEntity<Void> blindComment(
            @PathVariable int commentId,
            @RequestParam String contentType
    ) {
        adminCommentService.blindComment(commentId, contentType);
        return ResponseEntity.ok().build();
    }

    // 블라인드 해제
    @PutMapping("/{commentId}/unblind")
    public ResponseEntity<Void> unblindComment(
            @PathVariable int commentId,
            @RequestParam String contentType
    ) {
        adminCommentService.unblindComment(commentId, contentType);
        return ResponseEntity.ok().build();
    }
}
