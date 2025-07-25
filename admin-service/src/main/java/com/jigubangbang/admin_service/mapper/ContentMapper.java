package com.jigubangbang.admin_service.mapper;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface ContentMapper {
    // 게시글
    void blindBoardPost(@Param("id") int id);
    void blindFeedPost(@Param("id") int id);
    void unblindBoardPost(@Param("id") int id); 
    void unblindFeedPost(@Param("id") int id);
    
    // 댓글
    void blindBoardComment(@Param("id") int id);
    void blindFeedComment(@Param("id") int id);
    void blindMateComment(@Param("id") int id);
    void unblindBoardComment(@Param("id") int id);  
    void unblindFeedComment(@Param("id") int id);
    void unblindMateComment(@Param("id") int id); 

    // 그룹
    void blindMateGroup(@Param("id") int id);
    void blindInfoGroup(@Param("id") int id);
    void unblindMateGroup(@Param("id") int id);    
    void unblindInfoGroup(@Param("id") int id); 
}
