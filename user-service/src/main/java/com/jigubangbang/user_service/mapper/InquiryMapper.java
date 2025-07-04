package com.jigubangbang.user_service.mapper;

import com.jigubangbang.user_service.model.InquiryDto;
import com.jigubangbang.user_service.model.CreateInquiryDto;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface InquiryMapper {

    // 1:1 문의 등록
    int insertInquiry(@Param("userId") String userId, @Param("dto") CreateInquiryDto dto);

    // 문의 전체 목록 조회
    List<InquiryDto> selectInquiriesByUserId(@Param("userId") String userId);

    // 문의 상세 조회
    InquiryDto selectInquiryById(@Param("id") int id);

    // 문의 수정
    int updateInquiryByUser(@Param("id") int id, @Param("userId") String userId, @Param("dto") CreateInquiryDto dto);

    // 문의 삭제
    int deleteInquiryByUser(@Param("id") int id, @Param("userId") String userId);
}
