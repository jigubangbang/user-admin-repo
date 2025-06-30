package com.jigubangbang.user_service.service;

import com.jigubangbang.user_service.mapper.EmailMapper;
import com.jigubangbang.user_service.model.EmailDto;

import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

import java.sql.Timestamp;
import java.util.Random;

import org.springframework.mail.javamail.MimeMessageHelper;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;

@Service
@RequiredArgsConstructor
public class EmailService {

    private final JavaMailSender mailSender;
    private final EmailMapper emailMapper;
    private final TemplateEngine templateEngine;

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

        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");
            
            helper.setTo(email);
            helper.setSubject("[지구방방] 이메일 인증코드 안내");
            helper.setFrom(fromEmail);
            
            Context context = new Context();
            context.setVariable("code", code);
            String htmlContent = templateEngine.process("email-veri", context);
            
            helper.setText(htmlContent, true);
            mailSender.send(message);
            
        } catch (MessagingException e) {
            throw new RuntimeException("인증코드 전송 중 오류가 발생했습니다.", e);
        }
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

    public void sendFoundUserId(String email, String userId) {
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

            helper.setTo(email);
            helper.setSubject("[지구방방] 아이디 찾기 결과 안내");
            helper.setFrom(fromEmail);

            Context context = new Context();
            context.setVariable("userId", userId); 

            String htmlContent = templateEngine.process("find-id", context);
            helper.setText(htmlContent, true);

            mailSender.send(message);

        } catch (MessagingException e) {
            throw new RuntimeException("아이디 전송 중 오류가 발생했습니다.", e);
        }
    }

}