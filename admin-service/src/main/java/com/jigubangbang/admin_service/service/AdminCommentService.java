package com.jigubangbang.admin_service.service;

import com.jigubangbang.admin_service.chat_service.NotificationServiceClient;
import com.jigubangbang.admin_service.mapper.AdminCommentMapper;
import com.jigubangbang.admin_service.mapper.BlindCountMapper;
import com.jigubangbang.admin_service.model.AdminCommentDto;
import com.jigubangbang.admin_service.model.chat_service.BlindNotificationRequestDto;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

@Service
@RequiredArgsConstructor
public class AdminCommentService {

    private final AdminCommentMapper adminCommentMapper;
    private final BlindCountMapper blindCountMapper;
    private final NotificationServiceClient notificationServiceClient;

    // 댓글 목록 조회
    public List<AdminCommentDto> getAllComments(String contentType, String nickname, String status, String keyword,
            LocalDateTime startDate, LocalDateTime endDate) {
        List<AdminCommentDto> result = new ArrayList<>();

        if (contentType == null || contentType.equals("all")) {
            result.addAll(adminCommentMapper.getCommunityComments(nickname, status, keyword, startDate, endDate));
            result.addAll(adminCommentMapper.getFeedComments(nickname, status, keyword, startDate, endDate));
            result.addAll(adminCommentMapper.getGroupComments(nickname, status, keyword, startDate, endDate));
        } else if (contentType.equals("community")) {
            result.addAll(adminCommentMapper.getCommunityComments(nickname, status, keyword, startDate, endDate));
        } else if (contentType.equals("feed")) {
            result.addAll(adminCommentMapper.getFeedComments(nickname, status, keyword, startDate, endDate));
        } else if (contentType.equals("group")) {
            result.addAll(adminCommentMapper.getGroupComments(nickname, status, keyword, startDate, endDate));
        } else {
            throw new IllegalArgumentException("Invalid contentType: " + contentType);
        }

        result.sort(Comparator.comparing(AdminCommentDto::getCreatedAt).reversed());
        return result;
    }

    // 블라인드 처리
    public void blindComment(int commentId, String contentType) {
        AdminCommentDto commentInfo = adminCommentMapper.getCommentInfo(commentId, contentType);
        
        if ("BLINDED".equals(commentInfo.getStatus())) { // 이미 블라인드된 상태면 처리하지 않음 
            return;
        }
        
        switch (contentType) {
            case "community" -> adminCommentMapper.blindCommunityComment(commentId);
            case "feed" -> adminCommentMapper.blindFeedComment(commentId);
            case "group" -> adminCommentMapper.blindGroupComment(commentId);
            default -> throw new IllegalArgumentException("Invalid contentType: " + contentType);
        }
        
        blindCountMapper.increaseBlindCount(commentInfo.getUserId()); // blind_count + 1 

        BlindNotificationRequestDto notification = BlindNotificationRequestDto.builder()
        .userId(commentInfo.getUserId())
        .message("콘텐츠가 블라인드 처리되었습니다.\n자세한 사항은 1:1 문의를 통해 확인해 주세요.")
        .relatedUrl("/user/inquiry")
        .senderId(null)
        .build();

        notificationServiceClient.createBlindNotification(notification);
    }

    // 블라인드 해제
    public void unblindComment(int commentId, String contentType) {
        AdminCommentDto commentInfo = adminCommentMapper.getCommentInfo(commentId, contentType);
        
        if ("VISIBLE".equals(commentInfo.getStatus())) { // 이미 공개된 상태면 처리하지 않음 
            return;
        }
        
        switch (contentType) {
            case "community" -> adminCommentMapper.unblindCommunityComment(commentId);
            case "feed" -> adminCommentMapper.unblindFeedComment(commentId);
            case "group" -> adminCommentMapper.unblindGroupComment(commentId);
            default -> throw new IllegalArgumentException("Invalid contentType: " + contentType);
        }
        
        blindCountMapper.decreaseBlindCount(commentInfo.getUserId()); // blind_count - 1 
    }
}
