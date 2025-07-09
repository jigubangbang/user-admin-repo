package com.jigubangbang.payment_service.security;

import feign.RequestInterceptor;
import feign.RequestTemplate;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.security.Keys;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import io.jsonwebtoken.io.Decoders;
import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;

@Component
public class FeignClientInterceptor implements RequestInterceptor {

    private static final Logger logger = LoggerFactory.getLogger(FeignClientInterceptor.class);
    private static final String AUTHORIZATION_HEADER = "Authorization";
    private static final String TOKEN_PREFIX = "Bearer ";

    private final SecretKey jwtSecretKey;
    private final String jwtIssuer;

// ... (생략) ...

    public FeignClientInterceptor(
            @Value("${jwt.secret}") String secretKey,
            @Value("${jwt.issuer}") String issuer) {
        byte[] keyBytes = Decoders.BASE64.decode(secretKey);
        this.jwtSecretKey = Keys.hmacShaKeyFor(keyBytes);
        this.jwtIssuer = issuer;
    }

    @Override
    public void apply(RequestTemplate template) {
        ServletRequestAttributes attributes = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();

        if (attributes != null) {
            // 1. 사용자 요청이 있는 경우: 기존 토큰을 그대로 전달
            HttpServletRequest request = attributes.getRequest();
            String authorizationHeader = request.getHeader(AUTHORIZATION_HEADER);
            if (authorizationHeader != null && authorizationHeader.startsWith(TOKEN_PREFIX)) {
                template.header(AUTHORIZATION_HEADER, authorizationHeader);
                logger.debug("Propagating user's Authorization header to Feign request. Target: {}", template.feignTarget().name());
                return;
            }
        }

        // 2. 사용자 요청이 없는 경우 (웹훅, 스케줄러 등): 서비스 자체 토큰 발급
        logger.info("No user token found. Generating a service-to-service token for {}.", template.feignTarget().name());
        String serviceToken = createServiceToken();
        template.header(AUTHORIZATION_HEADER, TOKEN_PREFIX + serviceToken);
    }

    private String createServiceToken() {
        long now = System.currentTimeMillis();
        // 유효시간 5분
        Date validity = new Date(now + 300000);

        return Jwts.builder()
                .setIssuer(jwtIssuer)
                .setSubject("service-account-" + jwtIssuer)
                .setIssuedAt(new Date(now))
                .setExpiration(validity)
                .signWith(jwtSecretKey, SignatureAlgorithm.HS512)
                .compact();
    }
}
