package com.jigubangbang.admin_service.mapper;

import com.jigubangbang.admin_service.model.AdminCommentDto;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.time.LocalDateTime;
import java.util.List;

@Mapper
public interface AdminCommentMapper {

    // 커뮤니티 댓글 목록 조회
    List<AdminCommentDto> getCommunityComments(@Param("nickname") String nickname, @Param("status") String status,
            @Param("keyword") String keyword, @Param("startDate") LocalDateTime startDate,
            @Param("endDate") LocalDateTime endDate);

    // 피드 댓글 목록 조회        
    List<AdminCommentDto> getFeedComments(@Param("nickname") String nickname, @Param("status") String status,
            @Param("keyword") String keyword, @Param("startDate") LocalDateTime startDate,
            @Param("endDate") LocalDateTime endDate);

    // 모임 댓글 목록 조회
    List<AdminCommentDto> getGroupComments(@Param("nickname") String nickname, @Param("status") String status,
            @Param("keyword") String keyword, @Param("startDate") LocalDateTime startDate,
            @Param("endDate") LocalDateTime endDate);

    // 커뮤니티 댓글 블라인드 처리
    void blindCommunityComment(@Param("commentId") int commentId);
    void unblindCommunityComment(@Param("commentId") int commentId);

    // 피드 댓글 블라인드 처리
    void blindFeedComment(@Param("commentId") int commentId);
    void unblindFeedComment(@Param("commentId") int commentId);
    
    // 모임 댓글 블라인드 처리
    void blindGroupComment(@Param("commentId") int commentId);
    void unblindGroupComment(@Param("commentId") int commentId);
    
}
