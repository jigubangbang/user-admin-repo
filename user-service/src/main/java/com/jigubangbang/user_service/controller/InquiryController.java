package com.jigubangbang.user_service.controller;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import com.jigubangbang.user_service.model.AuthDto;
import com.jigubangbang.user_service.model.CreateInquiryDto;
import com.jigubangbang.user_service.model.InquiryDto;
import com.jigubangbang.user_service.service.InquiryService;
import com.jigubangbang.user_service.service.S3Service;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/user/inquiry")
@RequiredArgsConstructor
public class InquiryController {

    private final InquiryService inquiryService;
    private final S3Service s3Service;

    @PostMapping(consumes = "multipart/form-data")
    public ResponseEntity<?> create(
            @AuthenticationPrincipal AuthDto user,
            @RequestPart("dto") @Valid CreateInquiryDto dto,
            @RequestParam(value = "files", required = false) List<MultipartFile> files) throws IOException {

        System.out.println("인증된 사용자: " + (user != null ? user.getUsername() : "null"));

        if (files != null && !files.isEmpty()) {
            List<String> uploadedUrls = new ArrayList<>();

            for (MultipartFile file : files) {
                if (!file.isEmpty()) {
                    String s3Url = s3Service.uploadFile(file, "inquiry-attachments/");
                    uploadedUrls.add(s3Url);
                    System.out.println("전달된 파일: " + file.getOriginalFilename());
                    System.out.println("S3 업로드 완료: " + s3Url);
                }
            }

            dto.setAttachments(uploadedUrls);
            if (!uploadedUrls.isEmpty()) {
                dto.setAttachment(String.join(",", uploadedUrls));
            }
        } else {
            System.out.println("첨부된 파일 없음.");
        }

        if (user == null) {
            return ResponseEntity.status(401).body("로그인이 필요합니다.");
        }

        int id = inquiryService.createInquiry(user.getUsername(), dto);
        System.out.println("문의 등록 완료. ID: " + id);
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

    @PutMapping(value = "/{id}", consumes = "multipart/form-data")
    public ResponseEntity<?> update(@PathVariable int id,
            @AuthenticationPrincipal AuthDto user,
            @RequestPart("dto") @Valid CreateInquiryDto dto,
            @RequestParam(value = "files", required = false) List<MultipartFile> files) throws IOException {

        if (files != null && !files.isEmpty()) {
            List<String> uploadedUrls = new ArrayList<>();
            for (MultipartFile file : files) {
                if (!file.isEmpty()) {
                    String s3Url = s3Service.uploadFile(file, "inquiry-attachments/");
                    uploadedUrls.add(s3Url);
                    System.out.println("수정 시 업로드된 파일: " + file.getOriginalFilename());
                    System.out.println("S3 업로드 완료: " + s3Url);
                }
            }

            dto.setAttachments(uploadedUrls);
            if (!uploadedUrls.isEmpty()) {
                dto.setAttachment(String.join(",", uploadedUrls));
            }
        }

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
