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

        if (files != null && !files.isEmpty()) {
            List<CreateInquiryDto.AttachmentInfo> attachmentInfos = new ArrayList<>();

            for (MultipartFile file : files) {
                if (!file.isEmpty()) {
                    String s3Url = s3Service.uploadFile(file, "inquiry-attachments/");

                    CreateInquiryDto.AttachmentInfo info = new CreateInquiryDto.AttachmentInfo();
                    info.setOriginalName(file.getOriginalFilename());
                    info.setUrl(s3Url);
                    attachmentInfos.add(info);

                    System.out.println("등록된 파일: " + file.getOriginalFilename());
                    System.out.println("S3 업로드 완료: " + s3Url);
                }
            }

            dto.setAttachments(attachmentInfos);
            if (!attachmentInfos.isEmpty()) {
                dto.setAttachment(convertToJsonString(attachmentInfos));
            }
        } else {
            System.out.println("첨부된 파일 없음");
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

        List<CreateInquiryDto.AttachmentInfo> allAttachments = new ArrayList<>();
        
        // 1. 프론트에서 보낸 유지할 기존 파일들 추가
        if (dto.getKeepExistingFiles() != null) {
            allAttachments.addAll(dto.getKeepExistingFiles());
        }
        
        // 2. 새 파일들을 추가
        if (files != null && !files.isEmpty()) {
            for (MultipartFile file : files) {
                if (!file.isEmpty()) {
                    String s3Url = s3Service.uploadFile(file, "inquiry-attachments/");

                    CreateInquiryDto.AttachmentInfo info = new CreateInquiryDto.AttachmentInfo();
                    info.setOriginalName(file.getOriginalFilename());
                    info.setUrl(s3Url);
                    allAttachments.add(info);

                    System.out.println("수정 시 추가된 파일: " + file.getOriginalFilename());
                    System.out.println("S3 업로드 완료: " + s3Url);
                }
            }
        }
        
        // 3. 전체 파일 정보 설정
        dto.setAttachments(allAttachments);
        if (!allAttachments.isEmpty()) {
            dto.setAttachment(convertToJsonString(allAttachments));
        } else {
            dto.setAttachment(null); // 파일이 없으면 null
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

    // 클래스 끝부분에 추가
    private String convertToJsonString(List<CreateInquiryDto.AttachmentInfo> attachments) {
        if (attachments == null || attachments.isEmpty()) {
            return null;
        }

        StringBuilder json = new StringBuilder("[");
        for (int i = 0; i < attachments.size(); i++) {
            CreateInquiryDto.AttachmentInfo info = attachments.get(i);
            json.append("{")
                    .append("\"originalName\":\"").append(escapeJson(info.getOriginalName())).append("\",")
                    .append("\"url\":\"").append(escapeJson(info.getUrl())).append("\"")
                    .append("}");

            if (i < attachments.size() - 1) {
                json.append(",");
            }
        }
        json.append("]");
        return json.toString();
    }

    private String escapeJson(String str) {
        if (str == null)
            return "";
        return str.replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("\n", "\\n")
                .replace("\r", "\\r");
    }
}
