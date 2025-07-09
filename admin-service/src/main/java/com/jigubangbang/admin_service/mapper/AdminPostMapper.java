package com.jigubangbang.admin_service.mapper;

import com.jigubangbang.admin_service.model.AdminPostDto;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.time.LocalDateTime;
import java.util.List;

@Mapper
public interface AdminPostMapper {

    // 커뮤니티 게시글 목록 조회
    List<AdminPostDto> getCommunityPosts(
            @Param("nickname") String nickname,
            @Param("status") String status,
            @Param("keyword") String keyword,
            @Param("startDate") LocalDateTime startDate,
            @Param("endDate") LocalDateTime endDate);

    // 여행피드 게시글 목록 조회
    List<AdminPostDto> getFeedPosts(
            @Param("nickname") String nickname,
            @Param("status") String status,
            @Param("keyword") String keyword,
            @Param("startDate") LocalDateTime startDate,
            @Param("endDate") LocalDateTime endDate);

    // 커뮤니티 게시글 블라인드 처리
    void blindCommunityPost(@Param("postId") int postId);

    void unblindCommunityPost(@Param("postId") int postId);

    // 피드 게시글 블라인드 처리
    void blindFeedPost(@Param("postId") int postId);

    void unblindFeedPost(@Param("postId") int postId);
}
