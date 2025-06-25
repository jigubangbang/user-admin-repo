package com.jigubangbang.user_service.service;

import com.jigubangbang.user_service.mapper.EmailMapper;
import com.jigubangbang.user_service.model.EmailDto;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

import java.sql.Timestamp;
import java.util.Random;

@Service
@RequiredArgsConstructor
public class EmailService {

    private final JavaMailSender mailSender;
    private final EmailMapper emailMapper;

    @Value("${spring.mail.username}")
    private String fromEmail;

    // 이메일 인증 코드 전송 (유효시간 3분)
    public void sendVerificationCode(String email) {
        String code = generateCode();
        Timestamp expiresAt = new Timestamp(System.currentTimeMillis() + 3 * 60 * 1000); 

        EmailDto dto = new EmailDto();
        dto.setEmail(email);
        dto.setCode(code);
        dto.setExpiresAt(expiresAt);

        emailMapper.insertOrUpdateCode(dto);

        SimpleMailMessage message = new SimpleMailMessage();
        message.setTo(email);
        message.setSubject("[JIGUBANGBANG] 이메일 인증코드 안내");
        message.setText(
            "안녕하세요, 지구방방입니다.\n\n" +
            "회원가입을 위한 이메일 인증 코드입니다.\n" +
            "아래의 인증코드를 입력해 주세요.\n\n" +
            "인증코드: " + code + "\n" +
            "유효시간: 3분\n\n" +
            "인증 시간이 만료된 경우, 다시 요청해 주세요.\n\n" +
            "감사합니다!"
        );
        message.setFrom(fromEmail);

        mailSender.send(message);
    }

    // 인증 코드 검증
    public boolean verifyCode(String email, String code) {
        EmailDto saved = emailMapper.findByEmail(email);
        
        if (saved == null || saved.getCode() == null || saved.getExpiresAt() == null) {
            return false;
        }

        if (saved.getExpiresAt().before(new Timestamp(System.currentTimeMillis()))) {
            return false;
        }

        if (saved.getCode().equals(code)) {
            emailMapper.markEmailAsVerified(email);
            return true;
        }

        return false;
    }

    private String generateCode() {
        int number = 100000 + new Random().nextInt(900000);
        return String.valueOf(number);
    }
}