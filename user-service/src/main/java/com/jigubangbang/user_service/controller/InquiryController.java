package com.jigubangbang.user_service.controller;

import java.util.List;
import java.util.Map;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.jigubangbang.user_service.model.AuthDto;
import com.jigubangbang.user_service.model.CreateInquiryDto;
import com.jigubangbang.user_service.model.InquiryDto;
import com.jigubangbang.user_service.service.InquiryService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/user/inquiry")
@RequiredArgsConstructor
public class InquiryController {

    private final InquiryService inquiryService;

    @PostMapping
    public ResponseEntity<?> create(@AuthenticationPrincipal AuthDto user,
            @RequestBody @Valid CreateInquiryDto dto) {
        int id = inquiryService.createInquiry(user.getUsername(), dto);
        return ResponseEntity.ok(Map.of("id", id));
    }

    @GetMapping
    public ResponseEntity<List<InquiryDto>> getList(@AuthenticationPrincipal AuthDto user) {
        return ResponseEntity.ok(inquiryService.getInquiriesByUser(user.getUsername()));
    }

    @GetMapping("/{id}")
    public ResponseEntity<InquiryDto> getDetail(@PathVariable int id,
            @AuthenticationPrincipal AuthDto user) {
        return ResponseEntity.ok(inquiryService.getInquiryDetail(id, user.getUsername()));
    }

    @PutMapping("/{id}")
    public ResponseEntity<?> update(@PathVariable int id,
            @AuthenticationPrincipal AuthDto user,
            @RequestBody @Valid CreateInquiryDto dto) {
        inquiryService.updateInquiry(id, user.getUsername(), dto);
        return ResponseEntity.ok("문의가 수정되었습니다.");
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<?> delete(@PathVariable int id,
            @AuthenticationPrincipal AuthDto user) {
        inquiryService.deleteInquiry(id, user.getUsername());
        return ResponseEntity.noContent().build();
    }
}
