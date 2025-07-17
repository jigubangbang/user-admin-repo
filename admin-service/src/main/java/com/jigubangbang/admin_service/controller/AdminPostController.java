package com.jigubangbang.admin_service.controller;

import com.jigubangbang.admin_service.model.AdminPostDto;
import com.jigubangbang.admin_service.service.AdminPostService;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;

@RestController
@RequestMapping("/admin/posts")
@RequiredArgsConstructor
public class AdminPostController {

    private final AdminPostService adminPostService;

    // 게시글 목록 조회
    @GetMapping
    public ResponseEntity<List<AdminPostDto>> getAllPosts(
            @RequestParam(required = false) String contentType, // community | feed | all
            @RequestParam(required = false) String nickname,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime endDate
    ) {
        List<AdminPostDto> posts = adminPostService.getAllPosts(contentType, nickname, status, keyword, startDate, endDate);
        return ResponseEntity.ok(posts);
    }

    // 블라인드 처리
    @PutMapping("/{postId}/blind")
    public ResponseEntity<Void> blindPost(
            @PathVariable int postId,
            @RequestParam String contentType
    ) {
        adminPostService.blindPost(postId, contentType);
        return ResponseEntity.ok().build();
    }

    // 블라인드 해제
    @PutMapping("/{postId}/unblind")
    public ResponseEntity<Void> unblindPost(
            @PathVariable int postId,
            @RequestParam String contentType
    ) {
        adminPostService.unblindPost(postId, contentType);
        return ResponseEntity.ok().build();
    }
}
