package com.jigubangbang.admin_service.service;

import com.jigubangbang.admin_service.mapper.AdminPostMapper;
import com.jigubangbang.admin_service.model.AdminPostDto;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

@Service
@RequiredArgsConstructor
public class AdminPostService {

    private final AdminPostMapper adminPostMapper;

    // 게시글 목록 조회
    public List<AdminPostDto> getAllPosts(
            String contentType,
            String nickname,
            String status,
            String keyword,
            LocalDateTime startDate,
            LocalDateTime endDate) {
        List<AdminPostDto> result = new ArrayList<>();

        // contentType이 null 또는 all이면 둘 다 가져옴
        if (contentType == null || contentType.equals("all")) {
            result.addAll(adminPostMapper.getCommunityPosts(nickname, status, keyword, startDate, endDate));
            result.addAll(adminPostMapper.getFeedPosts(nickname, status, keyword, startDate, endDate));
        } else if (contentType.equals("community")) {
            result.addAll(adminPostMapper.getCommunityPosts(nickname, status, keyword, startDate, endDate));
        } else if (contentType.equals("feed")) {
            result.addAll(adminPostMapper.getFeedPosts(nickname, status, keyword, startDate, endDate));
        }

        // 최신순 정렬
        result.sort(Comparator.comparing(AdminPostDto::getCreatedAt).reversed());
        return result;
    }

    // 블라인드 처리
    public void blindPost(int postId, String contentType) {
        if (contentType.equals("community")) {
            adminPostMapper.blindCommunityPost(postId);
        } else if (contentType.equals("feed")) {
            adminPostMapper.blindFeedPost(postId);
        } else {
            throw new IllegalArgumentException("Invalid contentType: " + contentType);
        }
    }

    // 블라인드 해제
    public void unblindPost(int postId, String contentType) {
        if (contentType.equals("community")) {
            adminPostMapper.unblindCommunityPost(postId);
        } else if (contentType.equals("feed")) {
            adminPostMapper.unblindFeedPost(postId);
        } else {
            throw new IllegalArgumentException("Invalid contentType: " + contentType);
        }
    }
}
