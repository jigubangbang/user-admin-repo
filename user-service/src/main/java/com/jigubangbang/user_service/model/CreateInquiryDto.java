package com.jigubangbang.user_service.model;

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

    private String attachment;
}