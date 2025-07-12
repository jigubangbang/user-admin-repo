package com.jigubangbang.admin_service.mapper;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface ContentMapper {
    // 게시글
    void blindBoardPost(@Param("id") int id);
    void blindFeedPost(@Param("id") int id);
    
    // 댓글
    void blindBoardComment(@Param("id") int id);
    void blindFeedComment(@Param("id") int id);
    void blindMateComment(@Param("id") int id);

    // 그룹
    void blindMateGroup(@Param("id") int id);
    void blindInfoGroup(@Param("id") int id);
}
