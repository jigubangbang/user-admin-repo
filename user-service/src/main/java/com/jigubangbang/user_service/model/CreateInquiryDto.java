package com.jigubangbang.user_service.model;

import java.util.List;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class CreateInquiryDto {
    private int id;
    
    @NotBlank
    private String title;

    @NotBlank
    private String content;

    @NotNull
    private String category;

    private List<String> attachments;   // S3 URL 리스트
    private String attachment;          // DB 저장용 문자열
}